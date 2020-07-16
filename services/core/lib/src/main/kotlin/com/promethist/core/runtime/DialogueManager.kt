package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.builder.IrModel
import com.promethist.core.dialogue.BasicDialogue
import com.promethist.util.LoggerDelegate
import kotlin.math.roundToInt
import org.slf4j.Logger
import javax.inject.Inject
import com.promethist.core.model.Session.DialogueStackFrame as Frame

class DialogueManager : Component {
    @Inject
    lateinit var dialogueFactory: DialogueFactory

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context = with(context) {
        this@DialogueManager.logger.info("processing DM")
        if (session.dialogueStack.isEmpty()) {
            session.dialogueStack.push(
                    Frame(application.dialogueName, session.sessionId, application.properties, 0))
        }
        proceed(context)

        return context
    }

    private fun getIntentModels(currentFrame: Frame, context: Context): List<IrModel> {
        val node = getNode(currentFrame, context)
        require(node is Dialogue.UserInput)

        val models = mutableListOf(IrModel(node.dialogue.buildId, node.dialogue.dialogueName, node.id))
        if (!node.skipGlobalIntents) {
            //current global intents
            models.add(IrModel(node.dialogue.buildId, node.dialogue.dialogueName))
            //parents global intents
            context.session.dialogueStack.distinctBy { it.name } .forEach {
                val dialogue = dialogueFactory.get(it)
                models.add(IrModel(dialogue.buildId, dialogue.dialogueName))
            }
        }
        return models
    }

    private fun getIntentFrame(models: List<IrModel>, frame: Frame, context: Context): Frame {
        val (modelId, nodeId) = context.input.intent.name.split("#")
        val model = models.first { it.id == modelId }
        val dialogueName = model.dialogueName

        context.logger.info("IR match (model=${model.name}, id=${model.id}, answer=$nodeId, score=${context.input.intent.score}")

        return when {
            // intent is from current dialogue
            dialogueName == frame.name -> {
                frame.copy(nodeId = nodeId.toInt())
            }
            //intent is from parent dialogue
            context.session.dialogueStack.any { it.name == dialogueName } -> {
                context.session.dialogueStack.first { it.name == dialogueName }.copy(nodeId = nodeId.toInt())
            }
            else -> error("Dialogue $dialogueName matched by IR is not on current stack.")
        }
    }

    private fun getCommandFrame(frame: Frame, context: Context): Frame {
        val node = getNode(frame, context) as Dialogue.UserInput
        val dialogue = dialogueFactory.get(frame)

        val commands = node.commands + dialogue.globalCommands

        return commands.firstOrNull { it.command == context.input.command }?.let {
            frame.copy(nodeId = it.id)
        } ?: error("Command ${context.input.command} not found in dialogue.")
    }

    private fun getNode(frame: Frame, context: Context): Dialogue.Node =
            dialogueFactory.get(frame).apply { context.locale = locale }.node(frame.nodeId)

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun proceed(context: Context): Boolean = with(context) {
        var frame = session.dialogueStack.pop()
        var inputRequested: Boolean? = null
        var node: Dialogue.Node
        val processedNodes = mutableListOf<Dialogue.Node>()

        try {
            while (inputRequested == null) {
                if (processedNodes.size > 20) error("Too much steps in processing dialogue (infinite loop?)")

                node = getNode(frame, context)
                processedNodes.add(node)
                if (node.id < 0)
                    turn.attributes[Dialogue.clientNamespace].set("nodeId", node.id)

                when (node) {
                    is Dialogue.UserInput -> {
                        if (shouldProcessUserInput(processedNodes, node)) {
                            //first user input in turn
                            val irModels = getIntentModels(frame, context)
                            context.irModels = irModels

                            val transition = node.process(context)
                            frame = if (transition != null) {
                                frame.copy(nodeId = transition.node.id)
                            } else {
                                // intent recognition
                                processPipeline()

                                if (context.input.command != null) {
                                    getCommandFrame(frame, context)
                                } else {
                                    getIntentFrame(irModels, frame, context)
                                }
                            }
                        } else {
                            //last user input in turn
                            addExpectedPhrases(context, node.intents.asList())
                            frame.copy(nodeId = node.id).let {
                                turn.endFrame = it
                                session.dialogueStack.push(it)
                            }
                            inputRequested = true
                        }
                    }
                    is Dialogue.Repeat -> {
                        if (session.dialogueStack.isEmpty()) inputRequested = false
                        frame = session.dialogueStack.pop()
                        session.turns.last { it.endFrame == frame }
                                .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                    }
                    is Dialogue.Function -> {
                        val transition = node.exec(context)
                        frame = frame.copy(nodeId = transition.node.id)
                    }
                    is Dialogue.StopSession -> {
                        session.dialogueStack.clear()
                        inputRequested = false
                    }
                    is Dialogue.GoBack, is Dialogue.StopDialogue -> {
                        if (session.dialogueStack.isEmpty()) {
                            inputRequested = false
                        } else {
                            frame = session.dialogueStack.pop()
                            if (node is Dialogue.GoBack && node.repeat) {
                                session.turns.last { it.endFrame == frame }
                                        .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                            }
                        }
                    }
                    is Dialogue.SubDialogue -> {
                        val args = node.getConstructorArgs(context)
                        session.dialogueStack.push(frame.copy(nodeId = node.next.id))
                        frame = Frame(node.name, session.sessionId, args, 0)
                    }
                    is Dialogue.TransitNode -> {
                        when (node) {
                            is Dialogue.Response -> {
                                val text = node.getText(context)
                                if (node.dialogue is BasicDialogue) {
                                    Dialogue.codeRun(context, node) {
                                        (node.dialogue as BasicDialogue).addResponseItem(text, image = node.image, audio = node.audio, video = node.video, repeatable = node.isRepeatable)
                                    }
                                } else {
                                    turn.addResponseItem(text, image = node.image, audio = node.audio, video = node.video, repeatable = node.isRepeatable)
                                }
                            }
                            is Dialogue.GlobalIntent, is Dialogue.GlobalCommand -> {
                                session.dialogueStack.push(session.turns.last().endFrame)
                            }
                        }
                        frame = frame.copy(nodeId = node.next.id)
                    }
                }
            }
            return inputRequested

        } finally {
            logNodes(processedNodes, logger)
        }
    }

    private fun shouldProcessUserInput(processedNodes: MutableList<Dialogue.Node>, node: Dialogue.UserInput): Boolean =
            when (processedNodes.size) {
                // first user input in the turn
                1 -> processedNodes[0] == node
                // user input next to start node
                2 -> (processedNodes[0] is Dialogue.StartDialogue && processedNodes[1] == node)
                else -> false
            }

    private fun logNodes(nodes: List<Dialogue.Node>, logger: Logger) {
        if (nodes.isEmpty()) return
        var dialogueNodes: List<Dialogue.Node>
        var rest = nodes
        do {
            dialogueNodes = rest.takeWhile { it.dialogue.dialogueName == rest.first().dialogue.dialogueName }
            logger.info("passed nodes ${dialogueNodes.first().dialogue.dialogueName} >> " +
                    dialogueNodes.map { it.toString() }.reduce { acc, s -> "$acc > $s" })
            rest = rest.drop(dialogueNodes.size)
        } while (rest.isNotEmpty())
    }

    private fun addExpectedPhrases(context: Context, intents: Collection<Dialogue.Intent>) {
        if (intents.isNotEmpty()) {
            //note: google has limit 5000 (+100k per whole ASR request), we use lower value to be more comfortable with even longer phrases
            val maxPhrasesPerIntent = 2000 / intents.size
            intents.forEach { intent ->
                if (intent.utterances.size > maxPhrasesPerIntent) {
                    val rat = intent.utterances.size / maxPhrasesPerIntent.toFloat()
                    var idx = 0.0F
                    for (i in 0 until maxPhrasesPerIntent) {
                        context.expectedPhrases.add(ExpectedPhrase(intent.utterances[idx.roundToInt()]))
                        idx += rat
                    }
                } else {
                    context.expectedPhrases.addAll(intent.utterances.map { text -> ExpectedPhrase(text) })
                }
            }
        }

        logger.info("${context.expectedPhrases.size} expected phrase(s) added")
    }
}
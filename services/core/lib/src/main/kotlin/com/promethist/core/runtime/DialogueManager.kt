package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.builder.IntentModel
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
                    Frame(application.dialogue_id.toString(), session.sessionId, application.properties, 0))
        }
        proceed(context)

        return context
    }

    private fun getIntentModels(currentFrame: Frame, context: Context): List<IntentModel> {
        val node = getNode(currentFrame, context)
        require(node is AbstractDialogue.UserInput)

        val models = mutableListOf(IntentModel(node.dialogue.buildId, node.dialogue.dialogueId, node.id))
        if (!node.skipGlobalIntents) {
            //current global intents
            models.add(IntentModel(node.dialogue.buildId, node.dialogue.dialogueId))
            //parents global intents
            context.session.dialogueStack.distinctBy { it.id } .forEach {
                val dialogue = dialogueFactory.get(it)
                models.add(IntentModel(dialogue.buildId, dialogue.dialogueId))
            }
        }
        return models
    }

    private fun getIntentFrame(models: List<IntentModel>, frame: Frame, context: Context): Frame {
        val (modelId, nodeId) = context.input.intent.name.split("#")
        val model = models.first { it.id == modelId }
        val dialogueName = model.dialogueId

        context.logger.info("IR match (model=${model.name}, id=${model.id}, answer=$nodeId, score=${context.input.intent.score}")

        return when {
            // intent is from current dialogue
            dialogueName == frame.id -> {
                frame.copy(nodeId = nodeId.toInt())
            }
            //intent is from parent dialogue
            context.session.dialogueStack.any { it.id == dialogueName } -> {
                context.session.dialogueStack.first { it.id == dialogueName }.copy(nodeId = nodeId.toInt())
            }
            else -> error("Dialogue $dialogueName matched by IR is not on current stack.")
        }
    }

    private fun getCommandFrame(frame: Frame, context: Context): Frame {
        val node = getNode(frame, context) as AbstractDialogue.UserInput
        val dialogue = dialogueFactory.get(frame)

        val commands = node.commands + dialogue.globalCommands
        commands.firstOrNull { it.command == context.input.command }?.let {
            return frame.copy(nodeId = it.id)
        }

        context.session.dialogueStack.distinctBy { it.id }.reversed().forEach { f ->
            dialogueFactory.get(f).globalCommands.firstOrNull { it.command == context.input.command }?.let {
                return f.copy(nodeId = it.id)
            }
        }

        error("Action ${context.input.command} not found in dialogue.")
    }

    private fun getNode(frame: Frame, context: Context): AbstractDialogue.Node =
            dialogueFactory.get(frame).apply { context.locale = locale }.node(frame.nodeId)

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun proceed(context: Context): Boolean = with(context) {
        var frame = session.dialogueStack.pop()
        var inputRequested: Boolean? = null
        var node: AbstractDialogue.Node
        val processedNodes = mutableListOf<AbstractDialogue.Node>()

        try {
            while (inputRequested == null) {
                if (processedNodes.size > 20) error("Too much steps in processing dialogue (infinite loop?)")

                node = getNode(frame, context)
                processedNodes.add(node)
                if (node.id < 0)
                    turn.attributes[AbstractDialogue.defaultNamespace].set("nodeId", node.id)

                when (node) {
                    is AbstractDialogue.UserInput -> {
                        if (shouldProcessUserInput(processedNodes, node)) {
                            //first user input in turn
                            val intentModels = getIntentModels(frame, context)
                            context.intentModels = intentModels

                            val transition = node.process(context)
                            frame = if (transition != null) {
                                frame.copy(nodeId = transition.node.id)
                            } else {
                                // intent recognition
                                processPipeline()

                                if (context.input.command != null) {
                                    getCommandFrame(frame, context)
                                } else {
                                    getIntentFrame(intentModels, frame, context)
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
                    is AbstractDialogue.Repeat -> {
                        if (session.dialogueStack.isEmpty()) inputRequested = false
                        frame = session.dialogueStack.pop()
                        session.turns.last { it.endFrame == frame }
                                .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                    }
                    is AbstractDialogue.Function -> {
                        val transition = node.exec(context)
                        frame = frame.copy(nodeId = transition.node.id)
                    }
                    is AbstractDialogue.StopSession -> {
                        session.dialogueStack.clear()
                        inputRequested = false
                    }
                    is AbstractDialogue.GoBack -> {
                        if (session.dialogueStack.isEmpty()) {
                            inputRequested = false
                        } else {
                            frame = session.dialogueStack.pop()
                            if (node.repeat) {
                                session.turns.last { it.endFrame == frame }
                                        .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                            }
                        }
                    }
                    is AbstractDialogue.StopDialogue -> {
                        while (frame.id == node.dialogue.dialogueName) {
                            if (session.dialogueStack.isEmpty()) {
                                inputRequested = false
                                break
                            }
                            frame = session.dialogueStack.pop()
                        }
                    }
                    is AbstractDialogue.SubDialogue -> {
                        val args = node.getConstructorArgs(context)
                        session.dialogueStack.push(frame.copy(nodeId = node.next.id))
                        frame = Frame(node.dialogueId, session.sessionId, args, 0)
                    }
                    is AbstractDialogue.TransitNode -> {
                        when (node) {
                            is AbstractDialogue.Response -> {
                                val text = node.getText(context)
                                if (node.dialogue is BasicDialogue) {
                                    AbstractDialogue.run(context, node) {
                                        (node.dialogue as BasicDialogue).addResponseItem(text, image = node.image, audio = node.audio, video = node.video, repeatable = node.isRepeatable)
                                    }
                                } else {
                                    turn.addResponseItem(text, image = node.image, audio = node.audio, video = node.video, repeatable = node.isRepeatable)
                                }
                            }
                            is AbstractDialogue.GlobalIntent, is AbstractDialogue.GlobalCommand -> {
                                if (session.turns.isNotEmpty()) //it is empty only when GlobalIntent/Command is reached in first turn(UInput right after start)
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

    private fun shouldProcessUserInput(processedNodes: MutableList<AbstractDialogue.Node>, node: AbstractDialogue.UserInput): Boolean =
            when (processedNodes.size) {
                // first user input in the turn
                1 -> processedNodes[0] == node
                // user input next to start node
                2 -> (processedNodes[0] is AbstractDialogue.StartDialogue && processedNodes[1] == node)
                else -> false
            }

    private fun logNodes(nodes: List<AbstractDialogue.Node>, logger: Logger) {
        if (nodes.isEmpty()) return
        var dialogueNodes: List<AbstractDialogue.Node>
        var rest = nodes
        do {
            dialogueNodes = rest.takeWhile { it.dialogue.dialogueName == rest.first().dialogue.dialogueName }
            logger.info("passed nodes ${dialogueNodes.first().dialogue.dialogueName} >> " +
                    dialogueNodes.map { it.toString() }.reduce { acc, s -> "$acc > $s" })
            rest = rest.drop(dialogueNodes.size)
        } while (rest.isNotEmpty())
    }

    private fun addExpectedPhrases(context: Context, intents: Collection<AbstractDialogue.Intent>) {
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
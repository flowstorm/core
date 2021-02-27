package ai.flowstorm.core.runtime

import ai.flowstorm.core.*
import ai.flowstorm.core.model.IntentModel
import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.dialogue.BasicDialogue
import ai.flowstorm.core.repository.DialogueEventRepository
import ai.flowstorm.util.LoggerDelegate
import org.slf4j.Logger
import javax.inject.Inject
import kotlin.math.roundToInt
import ai.flowstorm.core.model.Session.DialogueStackFrame as Frame

class DialogueManager : Component {
    @Inject
    lateinit var dialogueFactory: DialogueFactory

    private val logger by LoggerDelegate()

    override fun process(context: Context): Context = with(context) {
        this@DialogueManager.logger.info("Processing DM")
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

        val models = mutableListOf(
            IntentModel(
                node.dialogue.buildId,
                node.dialogue.dialogueId,
                node.id
            )
        )
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
        val intentNodes = getIntentsByDialogueStack(frame, context)
        val recognizedEntities = context.input.entityMap.keys.filter { context.input.entityMap[it]?.isNotEmpty() ?: false }

        val intent = context.input.intents.firstOrNull { intent ->
            val nodeId = intent.name.split("#")[1]
            val requiredEntities = intentNodes.firstOrNull { it.id == nodeId.toInt() }?.entities ?: listOf()
            recognizedEntities.containsAll(requiredEntities)
        }?: error("No intent for the given input and recognized entities $recognizedEntities found.")

        val (modelId, nodeId) = intent.name.split("#")
        val model = models.first { it.id == modelId }
        val dialogueName = model.dialogueId

        context.logger.info("IR match (model=${model.name}, id=${model.id}, answer=$nodeId, score=${context.input.intent.score})")

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

    private fun getIntentsByDialogueStack(currentFrame: Frame, context: Context): List<AbstractDialogue.Intent> =
        context.session.dialogueStack.map { dialogueFactory.get(it).globalIntents }.flatten() +
                dialogueFactory.get(currentFrame).globalIntents +
                (getNode(currentFrame, context) as AbstractDialogue.UserInput).intents

    private fun markRequiredEntities(frame: Frame, context: Context) {
        val inputNode = getNode(frame, context) as AbstractDialogue.UserInput
        val entities = inputNode.intents.map { it.entities }.flatten().toSet()
        context.input.entityMap.values.flatten().forEach { it.required = entities.contains(it.className) }
    }

    private fun getActionFrame(frame: Frame, context: Context): Frame {
        val node = getNode(frame, context) as? AbstractDialogue.UserInput
        val dialogue = dialogueFactory.get(frame)

        val actions = (node?.actions ?: emptyArray()) + dialogue.globalActions
        actions.firstOrNull { it.action == context.input.action }?.let {
            return frame.copy(nodeId = it.id)
        }

        context.session.dialogueStack.distinctBy { it.id }.reversed().forEach { f ->
            dialogueFactory.get(f).globalActions.firstOrNull { it.action == context.input.action }?.let {
                return f.copy(nodeId = it.id)
            }
        }

        // If the action outofdomain is not found in dialogue continue with normal intents
        if (context.input.action == "outofdomain") {
            return getIntentFrame(context.intentModels as List<IntentModel>, frame, context)
        }
        error("Action ${context.input.action} not found in dialogue")
    }

    private fun getNode(frame: Frame, context: Context): AbstractDialogue.Node =
            dialogueFactory.get(frame).apply { context.locale = locale }.node(frame.nodeId)

    /**
     * @return true if next user input requested, false if conversation ended
     */
    private fun proceed(context: Context): Unit = with(context) {
        var frame = session.dialogueStack.pop()
        var node: AbstractDialogue.Node
        val processedNodes = mutableListOf<AbstractDialogue.Node>()
        try {
            loop@ while (true) {
                try {
                    if (processedNodes.size >= 40)
                        error("Too many steps (over 40) in dialogue turn (${processedNodes.size})")
                    node = getNode(frame, context)
                    if (node !is AbstractDialogue.UserInput && processedNodes.contains(node))
                        error("$node is repeating in turn")
                    processedNodes.add(node)
                    if (node.id < 0)
                        turn.attributes[AbstractDialogue.defaultNamespace].set("nodeId", node.id)
                    if (turn.inputId != null && turn.nextId == null)
                        turn.nextId = node.id

                    when (node) {
                        is AbstractDialogue.UserInput -> {
                            if (shouldProcessUserInput(processedNodes, node)) {
                                turn.inputId = node.id
                                //first user input in turn
                                val intentModels = getIntentModels(frame, context)
                                context.intentModels = intentModels

                                val transition = node.process(context)
                                frame = if (transition != null) {
                                    frame.copy(nodeId = transition.node.id)
                                } else {
                                    markRequiredEntities(frame, context)
                                    // process the rest of the pipeline components
                                    processPipeline()

                                    if (context.input.action != null) {
                                        getActionFrame(frame, context)
                                    } else {
                                        getIntentFrame(intentModels, frame, context)
                                    }
                                }
                            } else {
                                //last user input in turn
                                addExpectedPhrases(context, node.expectedPhrases, node.intents.asList())
                                context.turn.sttMode = node.sttMode ?: node.dialogue.sttMode
                                frame.copy(nodeId = node.id).let {
                                    turn.endFrame = it
                                    session.dialogueStack.push(it)
                                }
                                break@loop
                            }
                        }
                        is AbstractDialogue.Function -> {
                            val transition = node.exec(context)
                            frame = frame.copy(nodeId = transition.node.id)
                        }
                        is AbstractDialogue.StopSession -> {
                            session.dialogueStack.clear()
                            break@loop
                        }
                        is AbstractDialogue.Sleep -> {
                            frame.copy(nodeId = node.next.id).let {
                                turn.endFrame = it
                                session.dialogueStack.push(it)
                            }
                            sleepTimeout = node.timeout
                            break@loop
                        }
                        is AbstractDialogue.GoBack,
                        is AbstractDialogue.Repeat -> {
                            if (session.dialogueStack.isEmpty()) {
                                break@loop
                            } else {
                                frame = session.dialogueStack.pop()
                                if (node is AbstractDialogue.Repeat || (node as AbstractDialogue.GoBack).repeat) {
                                    session.turns.last { it.endFrame == frame }
                                        .responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                                }
                            }
                        }
                        is AbstractDialogue.StopDialogue -> {
                            while (frame.id == node.dialogue.dialogueId) {
                                if (session.dialogueStack.isEmpty()) {
                                    break@loop
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
                                    val background = node.dialogue.background ?: node.background
                                    with (node) {
                                        if (node.dialogue is BasicDialogue) {
                                            AbstractDialogue.run(context, node) {
                                                (node.dialogue as BasicDialogue)
                                                    .addResponseItem(text, image, audio, video, code, background, isRepeatable, ttsConfig)
                                            }
                                        } else {
                                            turn.addResponseItem(text, image, audio, video, code, background, isRepeatable, ttsConfig)
                                        }
                                    }
                                }
                                is AbstractDialogue.Command -> {
                                    turn.addResponseItem('#' + node.command, code = node.code, repeatable = false)
                                }
                                is AbstractDialogue.GlobalIntent,
                                is AbstractDialogue.GlobalAction -> {
                                    if (session.turns.isNotEmpty()) //it is empty only when GlobalIntent/Action is reached in first turn(UInput right after start)
                                        session.dialogueStack.push(session.turns.last().endFrame)
                                }
                            }
                            frame = frame.copy(nodeId = node.next.id)
                        }
                    }
                } catch (e: AbstractDialogue.DialogueScriptException) {
                    context.createDialogueEvent(e)
                    context.input.action = "error"
                    try {
                        frame = getActionFrame(frame, context)
                    } catch (fe: IllegalStateException) { // action error not found, let's throw original exception
                        throw e
                    }
                }
            }
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
            logger.info("Passed nodes ${dialogueNodes.first().dialogue.dialogueName} >> " +
                    dialogueNodes.map { it.toString() }.reduce { acc, s -> "$acc > $s" })
            rest = rest.drop(dialogueNodes.size)
        } while (rest.isNotEmpty())
    }

    private fun addExpectedPhrases(context: Context, expectedPhrases: List<ExpectedPhrase>, intents: Collection<AbstractDialogue.Intent>) {
        context.turn.expectedPhrases.addAll(expectedPhrases)
        if (intents.isNotEmpty()) {
            //note: google has limit 5000 (+100k per whole ASR request), we use lower value to be more comfortable with even longer phrases
            val maxPhrasesPerIntent = 2000 / intents.size
            intents.forEach { intent ->
                if (intent.utterances.size > maxPhrasesPerIntent) {
                    val rat = intent.utterances.size / maxPhrasesPerIntent.toFloat()
                    var idx = 0.0F
                    for (i in 0 until maxPhrasesPerIntent) {
                        context.turn.expectedPhrases.add(ExpectedPhrase(intent.utterances[idx.roundToInt()]))
                        idx += rat
                    }
                } else {
                    context.turn.expectedPhrases.addAll(intent.utterances.map { text -> ExpectedPhrase(text) })
                }
            }
        }

        logger.info("${context.turn.expectedPhrases.size} expected phrase(s) added")
    }
}
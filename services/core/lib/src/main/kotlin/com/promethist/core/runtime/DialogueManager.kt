package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.model.Turn
import com.promethist.util.LoggerDelegate

class DialogueManager(private val loader: Loader) : Component {

    private val logger by LoggerDelegate()
    private val dialogues: MutableMap<String, Dialogue> = mutableMapOf()

    private fun get(name: String, context: Context, args: Array<Any>? = null): Dialogue {
        val key = "$name:${context.session.sessionId}"
        return if (!dialogues.containsKey(key)) {
            logger.info("loading model $name")
            if (args == null)
                error("Missing arguments for creating model $name")
            val dialogue = loader.newObject<Dialogue>(name, *args!!)
            dialogue.loader = loader
            dialogue.logger = context.logger
            dialogues[key] = dialogue
            dialogue
        } else {
            dialogues[key]!!
        }
    }

    private fun set(name: String, context: Context, dialogue: Dialogue): Dialogue {
        val key = "$name:${context.session.sessionId}"
        dialogues[key] = dialogue
        return dialogue
    }

    override fun process(context: Context): Context = with (context) {
        this@DialogueManager.logger.info("processing DM")
        if (turn.dialogueStack.isEmpty()) {
            start(get("${context.session.application.dialogueName}/model", context,
                    context.session.application.properties.values.toTypedArray()), context)
        } else {
            proceed(context)
        }
        return context
    }

    private fun start(dialogue: Dialogue, context: Context): Boolean = with (context) {
        this@DialogueManager.logger.info("starting ${dialogue.name}\n" + dialogue.describe())
        dialogue.validate()
        set(dialogue.name, context, dialogue)
        turn.dialogueStack.push(Turn.DialogueStackFrame(dialogue.name))
        return proceed(context)
    }

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun proceed(context: Context): Boolean = with (context) {
        var frame = turn.dialogueStack.first()
        val dialogue = get(frame.name, context)
        var node = dialogue.node(frame.nodeId)
        if (node is Dialogue.UserInput) {
            val transition = node.process(context)
            if (transition != null) {
                node = transition.node
            } else {
                // intent recognition
                context.processPipeline()
                node = dialogue.node(turn.input.intent.name.toInt())
            }
        }
        var step = 0
        var inputRequested: Boolean? = null

        val processedNodes = mutableListOf<Dialogue.Node>()
        while (inputRequested == null) {
            if (step++ > 20) error("Too much steps in processing dialogue (infinite loop?)")

            frame.nodeId = node.id
            processedNodes.add(node)
            when (node) {
                is Dialogue.UserInput -> {
                    frame.skipGlobalIntents = node.skipGlobalIntents
                    node.intents.forEach { intent ->
                        context.expectedPhrases.addAll(intent.utterances.map { text -> ExpectedPhrase(text) })
                    }
                    inputRequested = true
                }
                is Dialogue.Repeat -> {
                    //TODO not tested yet!!!
                    var prevNodeId = 0
                    for (i in session.turns.size - 1 downTo 0) {
                        val prevFrame = session.turns[i].dialogueStack.first()
                        val prevNode = dialogue.node(prevFrame.nodeId)
                        if (prevFrame.name == dialogue.name) {
                            if (prevNode is Dialogue.Response && prevNode.isRepeatable)
                                prevNodeId = prevFrame.nodeId
                            else if (prevNodeId != 0)
                                break
                        }
                    }
                    frame.nodeId = prevNodeId
                }
                is Dialogue.Function -> {
                    val transition = node.exec(context)
                    node = transition.node
                }
                is Dialogue.StopSession -> {
                    turn.dialogueStack.clear()
                    inputRequested = false
                }
                is Dialogue.StopDialogue -> {
                    turn.dialogueStack.pop()
                    inputRequested =  if (turn.dialogueStack.isEmpty()) false else proceed(context)
                }
                is Dialogue.SubDialogue -> {
                    val subDialogue = node.createDialogue(context)
                    frame.nodeId = node.next.id
                    inputRequested = start(subDialogue, context)
                }
                is Dialogue.TransitNode -> {
                    if (node is Dialogue.Response) {
                        val text = node.getText(context)
                        val item = Response.Item(text, image = node.image, audio = node.audio, video = node.video)
                        turn.responseItems.add(item)
                    }
                    node = node.next
                }
            }
        }

        logger.info("passed ${dialogue.name} >> " +
                processedNodes.map { "${it::class.simpleName}#${it.id}" }.reduce { acc, s -> "$acc > $s" })

        return true
    }
}
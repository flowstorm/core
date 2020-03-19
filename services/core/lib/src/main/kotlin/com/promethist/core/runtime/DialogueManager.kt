package com.promethist.core.runtime

import com.promethist.core.nlp.Context
import com.promethist.core.model.Turn
import com.promethist.core.model.Dialogue
import com.promethist.core.model.MessageItem
import com.promethist.core.nlp.Component
import org.slf4j.LoggerFactory

class DialogueManager(private val loader: Loader) : Component {

    private var logger = LoggerFactory.getLogger(this::class.qualifiedName)
    private val dialogues: MutableMap<String, Dialogue> = mutableMapOf()

    private fun get(name: String, context: Context, args: Array<Any>? = null): Dialogue {
        val key = "$name:${context.session.sessionId}"
        return if (!dialogues.containsKey(key)) {
            logger.info("loading $name")
            if (args == null)
                error("Missing arguments for creating model $name")
            val dialogue = loader.newObject<Dialogue>(name, *args!!)
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
        context.sessionEnded = if (turn.dialogueStack.isEmpty()) {
            start(get("${context.session.application.dialogueName}/model", context,
                    context.session.application.properties.values.toTypedArray()), context)
        } else {
            turn.dialogueStack.first.nodeId = turn.input.text.toInt()
            trace(context)
        }
        return context
    }

    private fun start(dialogue: Dialogue, context: Context): Boolean = with (context) {
        logger.info("starting ${dialogue.name}\n" + dialogue.describe())
        dialogue.validate()
        set(dialogue.name, context, dialogue)
        turn.dialogueStack.push(Turn.DialogueStackFrame(dialogue.name))
        return trace(context)
    }

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun trace(context: Context): Boolean = with (context) {
        var frame = turn.dialogueStack.first()
        val dialogue = get(frame.name, context)
        var node = dialogue.node(frame.nodeId)
        var step = 0
        while (step++ < 20) {
            frame.nodeId = node.id
            logger.info(frame.toString())
            when (node) {
                is Dialogue.UserInput ->
                    return true
                is Dialogue.Function -> {
                    val transition = node.exec(context)
                    node = transition.node
                }
                is Dialogue.StopSession -> {
                    turn.dialogueStack.clear()
                    return false
                }
                is Dialogue.StopDialogue -> {
                    turn.dialogueStack.pop()
                    return if (turn.dialogueStack.isEmpty()) false else trace(context)
                }
                is Dialogue.SubDialogue -> {
                    val subDialogue = node.createDialogue(context)
                    frame.nodeId = node.next.id
                    return start(subDialogue, context)
                }
                is Dialogue.TransitNode -> {
                    if (node is Dialogue.Response) {
                        val text = node.getText(context)
                        val item = MessageItem(text, image = node.image, audio = node.audio, video = node.video)
                        turn.responseItems.add(item)
                    }
                    node = node.next
                }
            }
        }
        error("Too much steps in processing dialogue (infinite loop?)")
    }
}
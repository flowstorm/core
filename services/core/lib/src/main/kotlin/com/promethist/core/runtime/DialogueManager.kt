package com.promethist.core.runtime

import com.promethist.core.Context
import com.promethist.core.model.Turn
import com.promethist.core.model.Dialogue
import com.promethist.core.model.MessageItem
import org.slf4j.LoggerFactory

class DialogueManager(private val loader: Loader) {

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

    fun start(dialogueName: String, context: Context, args: Array<Any>) =
        start(get(dialogueName, context, args), context)

    fun proceed(context: Context): Boolean = with (context) {
        val frame = turn.dialogueStack.first
        //FIXME do intent reco instead of temporarily using context.input as nodeId
        //TODO call intent reco model with key equal to frame.hashCode
        turn.dialogueStack.first.nodeId = turn.input.toInt()
        val dialogue = get(turn.dialogueStack.first().name, context)
        return process(context)
    }

    private fun start(dialogue: Dialogue, context: Context): Boolean = with (context) {
        logger.info("starting ${dialogue.name}\n" + dialogue.describe())
        dialogue.validate()
        set(dialogue.name, context, dialogue)
        turn.dialogueStack.push(Turn.DialogueStackFrame(dialogue.name))
        return process(context)
    }

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun process(context: Context): Boolean = with (context) {
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
                    return if (turn.dialogueStack.isEmpty()) false else process(context)
                }
                is Dialogue.SubDialogue -> {
                    val subDialogue = node.createDialogue(context)
                    frame.nodeId = node.next.id
                    return start(subDialogue, context)
                }
                is Dialogue.TransitNode -> {
                    if (node is Dialogue.Response) {
                        val text = node.getText(context)
                        val item = MessageItem(text)
                        if (node is Dialogue.AudioResponse)
                            item.audio = node.audio
                        if (node is Dialogue.ImageResponse)
                            item.image = node.image
                        turn.responseItems.add(item)
                    }
                    node = node.next
                }
            }
        }
        error("Too much steps in processing dialogue (infinite loop?)")
    }
}
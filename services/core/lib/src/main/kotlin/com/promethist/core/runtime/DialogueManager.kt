package com.promethist.core.runtime

import com.promethist.core.model.Context
import com.promethist.core.model.Dialogue
import com.promethist.core.model.MessageItem
import com.promethist.core.model.Session
import org.slf4j.LoggerFactory

class DialogueManager(private val loader: Loader) {

    private var logger = LoggerFactory.getLogger(this::class.qualifiedName)
    private val dialogues: MutableMap<String, Dialogue> = mutableMapOf()

    private fun load(name: String, session: Session, args: Array<Any>? = null): Dialogue {
        val key = "$name:${session.sessionId}"
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

    private fun save(name: String, session: Session, dialogue: Dialogue): Dialogue {
        val key = "$name:${session.sessionId}"
        dialogues[key] = dialogue
        return dialogue
    }

    fun start(dialogueName: String, session: Session, context: Context, args: Array<Any>) =
        start(load(dialogueName, session, args), Dialogue.Scope(session, context, logger))

    fun proceed(session: Session, context: Context): Boolean {
        //FIXME do intent reco instead of temporarily using context.input as nodeId
        context.dialogueStack.first.nodeId = context.input.toInt()
        val dialogue = load(context.dialogueStack.first().name, session)
        return process(Dialogue.Scope(session, context, logger))
    }

    private fun start(dialogue: Dialogue, scope: Dialogue.Scope): Boolean = with (scope) {
        logger.info("starting ${dialogue.name}\n" + dialogue.describe())
        dialogue.validate()
        save(dialogue.name, session, dialogue)
        context.dialogueStack.push(Context.DialogueStackFrame(dialogue.name))
        return process(scope)
    }

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun process(scope: Dialogue.Scope): Boolean = with (scope) {
        var frame = context.dialogueStack.first()
        val dialogue = load(frame.name, session)
        var node = dialogue.node(frame.nodeId)
        var step = 0
        while (step++ < 20) {
            frame.nodeId = node.id
            logger.info(frame.toString())
            when (node) {
                is Dialogue.UserInput ->
                    return true
                is Dialogue.Function -> {
                    val transition = node.exec(scope)
                    node = transition.node
                }
                is Dialogue.StopSession -> {
                    context.dialogueStack.clear()
                    return false
                }
                is Dialogue.StopDialogue -> {
                    context.dialogueStack.pop()
                    if (context.dialogueStack.isEmpty())
                        return false
                    else
                        //frame = context.dialogueStack.first()
                        return process(scope)
                }
                is Dialogue.SubDialogue -> {
                    val subDialogue = node.createDialogue(scope)
                    frame.nodeId = node.next.id
                    return start(subDialogue, scope)
                }
                is Dialogue.TransitNode -> {
                    if (node is Dialogue.Response) {
                        val text = node.getText(scope)
                        val item = MessageItem(text)
                        if (node is Dialogue.AudioResponse)
                            item.audio = node.audio
                        if (node is Dialogue.ImageResponse)
                            item.image = node.image
                        context.responseItems.add(item)
                    }
                    node = node.next
                }
            }
        }
        error("Too much steps in processing dialogue (infinite loop?)")
    }
}
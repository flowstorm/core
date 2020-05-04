package com.promethist.core.runtime

import com.promethist.core.*
import com.promethist.core.dialogue.Dialogue
import com.promethist.core.builder.IrModel
import com.promethist.core.model.Session
import com.promethist.core.type.PropertyMap
import com.promethist.util.LoggerDelegate
import kotlin.math.roundToInt

class DialogueManager(private val loader: Loader) : Component {

    private val logger by LoggerDelegate()
    private val dialogues: MutableMap<String, Dialogue> = mutableMapOf()

    private fun get(name: String, context: Context, args: PropertyMap = mapOf()): Dialogue {
        val key = "$name:${context.session.sessionId}"
        return if (!dialogues.containsKey(key)) {
            logger.info("loading model $name")
            val dialogue = loader.newObjectWithArgs<Dialogue>(name, args)
            dialogue.loader = loader
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
        if (session.dialogueStack.isEmpty()) {
            start(get("${context.session.application.dialogueName}/model", context,
                    context.session.application.properties), context)
        } else {
            proceed(context)
        }
        return context
    }

    private fun start(dialogue: Dialogue, context: Context): Boolean = with (context) {
        this@DialogueManager.logger.info("starting dialogue ${dialogue.name} with following nodes:\n" + dialogue.describe())
        dialogue.validate()
        set(dialogue.name, context, dialogue)
        session.dialogueStack.push(Session.DialogueStackFrame(dialogue.name))
        return proceed(context)
    }

    /**
     * @return true if next user input requested, false if session ended
     */
    private fun proceed(context: Context): Boolean = with (context) {
        val frame = session.dialogueStack.pop()
        val dialogue = get(frame.name, context)
        var node = dialogue.node(frame.nodeId)
        if (node is Dialogue.UserInput) {
            val models = mutableListOf(IrModel(dialogue.buildId, dialogue.name, node.id))
            if (!node.skipGlobalIntents) models.add(IrModel(dialogue.buildId, dialogue.name, null))

            context.irModels = models

            val transition = node.process(context)
            node = if (transition != null) {
                transition.node
            } else {
                // intent recognition
                processPipeline()
                dialogue.intentNode(this)
            }
        }
        var step = 0
        var inputRequested: Boolean? = null

        val processedNodes = mutableListOf<Dialogue.Node>()
        while (inputRequested == null) {
            if (step++ > 20) error("Too much steps in processing dialogue (infinite loop?)")

            processedNodes.add(node)
            when (node) {
                is Dialogue.UserInput -> {
                    addExpectedPhrases(context, node.intents.asList())
                    frame.copy(nodeId = node.id).let {
                        turn.endFrame = it
                        session.dialogueStack.push(it)
                    }

                    inputRequested = true
                }
                is Dialogue.Repeat -> {
                    session.turns.last { it.endFrame!!.name == frame.name }.let { lastTurn ->
                        lastTurn.responseItems.forEach { if (it.repeatable) turn.responseItems.add(it) }
                        turn.endFrame = lastTurn.endFrame
                        session.dialogueStack.push(lastTurn.endFrame)
                    }

                    inputRequested = true
                }
                is Dialogue.Function -> {
                    val transition = node.exec(context)
                    node = transition.node
                }
                is Dialogue.StopSession -> {
                    session.dialogueStack.clear()
                    inputRequested = false
                }
                is Dialogue.StopDialogue -> {
                    inputRequested =  if (session.dialogueStack.isEmpty()) false else proceed(context)
                }
                is Dialogue.SubDialogue -> {
                    val subDialogue = node.createDialogue(context)
                    session.dialogueStack.push(frame.copy(nodeId = node.next.id))
                    inputRequested = start(subDialogue, context)
                }
                is Dialogue.TransitNode -> {
                    if (node is Dialogue.Response) {
                        val text = node.getText(context)
                        turn.addResponseItem(text, node.image, node.audio, node.video, repeatable = node.isRepeatable)
                    }
                    node = node.next
                }
            }
        }

        logger.info("passed nodes ${dialogue.name} >> " +
                processedNodes.map { it.toString() }.reduce { acc, s -> "$acc > $s" })

        return inputRequested
    }

    private fun addExpectedPhrases(context: Context, intents: Collection<Dialogue.Intent>) {
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
        logger.info("${context.expectedPhrases.size} expected phrase(s) added")
    }
}
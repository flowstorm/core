package ai.flowstorm.core.runtime

import ai.flowstorm.core.dialogue.AbstractDialogue
import ai.flowstorm.core.model.Session.DialogueStackFrame
import ai.flowstorm.core.type.PropertyMap
import ai.flowstorm.util.LoggerDelegate
import javax.inject.Inject

class DialogueFactory {

    @Inject
    lateinit var loader:Loader
    private val logger by LoggerDelegate()
    private val dialogues: MutableMap<Triple<String?, String, PropertyMap>, AbstractDialogue> = mutableMapOf()

    private fun create(id: String, args: PropertyMap): AbstractDialogue {
        logger.info("Creating new instance $id $args")
        val dialogue = loader.newObjectWithArgs<AbstractDialogue>(id + "/model", args)
        dialogue.loader = loader
        dialogue.validate()
        return dialogue
    }

    fun get(id: String, buildId: String, args: PropertyMap): AbstractDialogue {
        logger.debug("Getting instance $id $args")
        val triple = Triple(id, buildId,  args)
        return dialogues.getOrPut(triple) {create(triple.first, triple.third)}
    }

    fun get(frame: DialogueStackFrame): AbstractDialogue =
            get(frame.id ?: frame.name ?: error("Missing frame identification"), frame.buildId, frame.args)
}

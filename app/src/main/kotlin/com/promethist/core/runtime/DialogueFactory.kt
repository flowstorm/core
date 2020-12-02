package com.promethist.core.runtime

import com.promethist.core.dialogue.AbstractDialogue
import com.promethist.core.model.Session.DialogueStackFrame
import com.promethist.core.type.PropertyMap
import com.promethist.util.LoggerDelegate
import javax.inject.Inject

class DialogueFactory {

    @Inject
    lateinit var loader:Loader
    private val logger by LoggerDelegate()
    private val dialogues: MutableMap<Triple<String?, String, PropertyMap>, AbstractDialogue> = mutableMapOf()

    private fun create(id: String, args: PropertyMap): AbstractDialogue {
        logger.info("creating new instance $id $args")
        val dialogue = loader.newObjectWithArgs<AbstractDialogue>(id + "/model", args)
        dialogue.loader = loader
        dialogue.validate()
        return dialogue
    }

    fun get(id: String, buildId: String, args: PropertyMap): AbstractDialogue {
        logger.debug("getting instance $id $args")
        val triple = Triple(id, buildId,  args)
        return dialogues.getOrPut(triple) {create(triple.first, triple.third)}
    }

    fun get(frame: DialogueStackFrame): AbstractDialogue =
            get(frame.id ?: frame.name ?: error("missing frame identification"), frame.buildId, frame.args)
}

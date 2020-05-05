package com.promethist.core.runtime

import com.promethist.core.dialogue.Dialogue
import com.promethist.core.model.Session.DialogueStackFrame
import com.promethist.core.type.PropertyMap
import com.promethist.util.LoggerDelegate

class DialogueFactory(private val loader: Loader) {

    private val logger by LoggerDelegate()
    private val dialogues: MutableMap<Triple<String, String, PropertyMap>, Dialogue> = mutableMapOf()

    private fun create(name: String, args: PropertyMap): Dialogue {
        logger.info("creating new instance $name $args")
        val dialogue = loader.newObjectWithArgs<Dialogue>(name + "/model", args)
        dialogue.loader = loader
        dialogue.validate()
        return dialogue
    }

    fun get(name: String, buildId:String, args: PropertyMap): Dialogue {
        logger.info("loading instance $name $args")
        val triple = Triple(name, buildId,  args)
        return dialogues.getOrPut(triple) {create(triple.first, triple.third)}
    }

    fun get(frame: DialogueStackFrame): Dialogue = get(frame.name, frame.buildId,  frame.args)
}

package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import kotlin.reflect.full.primaryConstructor

open class DialogueApi(val dialogue: BasicDialogue) : Api() {

    companion object {

        val instances = mutableMapOf<String, DialogueApi>()

        inline fun <reified T : DialogueApi> get(dialogue: BasicDialogue): T =
                instances.getOrPut(T::class.java.canonicalName + "@" + dialogue.dialogueName) {
                    T::class.primaryConstructor!!.call(dialogue)
                } as T
    }
}
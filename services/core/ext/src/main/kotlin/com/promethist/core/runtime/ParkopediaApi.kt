package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*

class ParkopediaApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private fun withCustom(data: Dynamic): DynamicMutableList {
        val list = DynamicMutableList(data)
        val custom = dialogue.context.session.attributes["parkopedia"]["parking"]
        if (custom != null) {
            (custom as MemoryMutableList<Dynamic>).forEach { list.add(it.value) }
        }
        return list
    }

    fun addMockedData(vararg data: Dynamic) = with(dialogue) {
        val memoryList = MemoryMutableList(data.map { Memory(it) })
        context.session.attributes["parkopedia"].put("parking", Memorable.pack(memoryList))
    }

    fun nearParking(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): DynamicMutableList =
            withCustom(load("/test/parkopedia/rawMunich.json"))

}

val BasicDialogue.parkopedia get() = DialogueApi.get<ParkopediaApi>(this)
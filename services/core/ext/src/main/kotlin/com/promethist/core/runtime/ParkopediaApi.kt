package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.*

class ParkopediaApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {

    private fun withCustom(data: List<Dynamic>): DynamicMutableList {
        val dynamicList = DynamicMutableList(data)
        val custom = dialogue.context.session.attributes["parkopedia"]["parking"]
        if (custom != null) {
            (custom as MemoryMutableList<Dynamic>).forEach { dynamicList.add(it.value) }
        }
        return dynamicList
    }

    fun addMockedData(vararg data: Dynamic) = with(dialogue) {
        val memoryList = MemoryMutableList(data.map { Memory(it) })
        context.session.attributes["parkopedia"].put("parking", Memorable.pack(memoryList))
    }

    fun nearParking(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): DynamicMutableList {
        var samples = (load<Dynamic>("/test/parkopedia/rawMunich.json")("locations.all") as Iterable<PropertyMap>).map { Dynamic(it["properties"] as PropertyMap) }
        return withCustom(samples)
    }

}

val BasicDialogue.parkopedia get() = DialogueApi.get<ParkopediaApi>(this)
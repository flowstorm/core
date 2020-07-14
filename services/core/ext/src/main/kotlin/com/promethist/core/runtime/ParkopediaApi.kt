package com.promethist.core.runtime

import com.promethist.core.dialogue.BasicDialogue
import com.promethist.core.type.Dynamic

class ParkopediaApi(dialogue: BasicDialogue) : DialogueApi(dialogue) {
    fun nearParking(milesRadius: Int = 20, additionalParameters: Map<String, Any> = mapOf()): Dynamic =
            load("/test/parkopedia/rawMunich.json")

}

val BasicDialogue.parkopedia get() = DialogueApi.get<ParkopediaApi>(this)
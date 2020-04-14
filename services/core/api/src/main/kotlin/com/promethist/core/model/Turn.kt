package com.promethist.core.model

import com.promethist.core.Input
import com.promethist.core.Response
import com.promethist.core.type.Dynamic
import java.util.*

data class Turn(
        var input: Input,
        val datetime: Date = Date(),
        var attributes: Dynamic = Dynamic(),
        val responseItems: MutableList<Response.Item> = mutableListOf()
) {

    fun addResponseItem(text: String, image: String? = null, audio: String? = null, video: String? = null) {
        val plainText = text.replace(Regex("\\<.*?\\>"), "")
        val item = Response.Item(plainText,
                ssml = if (text != plainText) text else null,
                image = image, audio = audio, video = video)
        responseItems.add(item)
    }
}

package com.promethist.core.model

import com.promethist.core.Input
import com.promethist.core.Response
import com.promethist.core.model.Session.DialogueStackFrame
import com.promethist.core.type.Attributes
import java.util.*

data class Turn(
        var input: Input,
        val datetime: Date = Date(),
        var attributes: Attributes = Attributes(),
        var endFrame: DialogueStackFrame? = null, //where the turn ends (input node)
        val responseItems: MutableList<Response.Item> = mutableListOf(),
        val log: MutableList<LogEntry> = mutableListOf()
) {

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, repeatable: Boolean = true) {
        val plainText = text?.replace(Regex("\\<.*?\\>"), "")
        val item = Response.Item(plainText,
                ssml = if (text != plainText) text else null,
                image = image, audio = audio, video = video,
                repeatable = repeatable
        )
        item.ssml = item.ssml?.replace(Regex("<image.*?src=\"(.*?)\"[^\\>]+>")) {
            item.image = it.groupValues[1]
            ""
        }
        item.ssml = item.ssml?.replace(Regex("<video.*?src=\"(.*?)\"[^\\>]+>")) {
            item.video = it.groupValues[1]
            ""
        }

        responseItems.add(item)
    }
}

package org.promethist.core.model

import org.promethist.core.ExpectedPhrase
import org.promethist.core.Input
import org.promethist.core.Response
import org.promethist.core.model.Session.DialogueStackFrame
import org.promethist.core.type.Attributes
import org.promethist.core.type.DateTime
import org.promethist.core.type.PropertyMap
import java.time.ZoneId
import java.util.*

data class Turn(
        var input: Input,
        val request: Request = Request(),
        val datetime: Date = Date(),
        var attributes: Attributes = Attributes(),
        var endFrame: DialogueStackFrame? = null, //where the turn ends (input node)
        val responseItems: MutableList<Response.Item> = mutableListOf(),
        @Transient val expectedPhrases: MutableList<ExpectedPhrase> = mutableListOf(),
        var sttMode: SttConfig.Mode? = null,
        var duration: Long? = null,
        val log: MutableList<LogEntry> = mutableListOf()
) {
    data class Request(var attributes: PropertyMap? = null)

    val time: DateTime get() = DateTime.ofInstant(datetime.toInstant(), ZoneId.systemDefault())

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, code: String? = null, background: String? = null, repeatable: Boolean = true, voice: Voice?) =
        addResponseItem(text, image, audio, video, code, background, repeatable, voice?.config)

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, code: String? = null, background: String? = null, repeatable: Boolean = true, ttsConfig: TtsConfig? = null) {
        val plainText = text?.replace(Regex("\\<.*?\\>"), "")
        val item = Response.Item(plainText,
                ssml = if (text != plainText) text else null,
                image = image,
                audio = audio,
                video = video,
                code = code,
                background = background,
                repeatable = repeatable,
                ttsConfig = ttsConfig
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

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, background: String? = null, repeatable: Boolean = true, voice: Voice? = null) {
        addResponseItem(text, image, audio, video, null, background, repeatable, voice)
    }
}

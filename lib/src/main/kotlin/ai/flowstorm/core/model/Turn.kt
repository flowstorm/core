package ai.flowstorm.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import ai.flowstorm.core.ExpectedPhrase
import ai.flowstorm.core.Input
import ai.flowstorm.core.Response
import ai.flowstorm.core.model.Session.DialogueStackFrame
import ai.flowstorm.core.type.Attributes
import ai.flowstorm.core.type.DateTime
import ai.flowstorm.core.type.PropertyMap
import ai.flowstorm.common.model.TimeEntity
import java.time.ZoneId
import java.util.*

data class Turn(
    override val _id: Id<Turn> = newId(),
    val session_id: Id<Session>? = null, // cannot be NullId due to backward compatibility with turns stored in session entity
    val dialogue_id: Id<DialogueModel>? = null, // same issue
    var input: Input,
    var inputId: Int? = null,
    var nextId: Int? = null,
    val request: Request = Request(),
    override var datetime: Date = Date(),
    var attributes: Attributes = Attributes(),
    var endFrame: DialogueStackFrame? = null, //where the turn ends (input node)
    val responseItems: MutableList<Response.Item> = mutableListOf(),
    @Transient val expectedPhrases: MutableList<ExpectedPhrase> = mutableListOf(),
    var sttMode: SttConfig.Mode? = null,
    var duration: Long? = null,
    val log: MutableList<LogEntry> = mutableListOf()
) : TimeEntity<Turn> {
    data class Request(var attributes: PropertyMap? = null)

    val time: DateTime get() = DateTime.ofInstant(datetime.toInstant(), ZoneId.systemDefault())

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, code: String? = null, background: String? = null, repeatable: Boolean = true, voice: Voice?) =
        addResponseItem(text, image, audio, video, code, background, repeatable, voice?.config)

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, code: String? = null, background: String? = null, repeatable: Boolean = true, ttsConfig: TtsConfig? = null) {
        val plainText = text?.let {
            it.replace(Regex("\\<.*?\\>"), "")
                .trim()
                .capitalize()
                .replace(Regex("[\\s]+"), " ")
                .replace(Regex(",\$"), "...")
                .replace(Regex("([^\\.\\!\\?…:])\$"), "$1.")
                .replace(Regex(",([\\p{L}])"), ", $1")
                .replace(Regex("([^\\.])\\.\\.([^\\.])"), "$1.$2")
                .replace(Regex("([\\!\\?] )(\\D)")) { m -> m.groupValues[1] + m.groupValues[2].toUpperCase() }
                .replace(Regex("([^\\.])[\\.]{2}$"), "$1.")
                .replace(" .", ".")
                .replace(" ,", ",")
                .replace(",,", ",")
        }
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

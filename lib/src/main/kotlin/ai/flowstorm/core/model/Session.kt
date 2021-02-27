package ai.flowstorm.core.model

import org.litote.kmongo.Id
import org.litote.kmongo.newId
import ai.flowstorm.core.model.metrics.Metric
import ai.flowstorm.core.type.*
import ai.flowstorm.common.model.TimeEntity
import java.util.*

data class  Session(
        override val _id: Id<Session> = newId(),
        override var datetime: Date = Date(),
        val sessionId: String,
        val test: Boolean = false,
        var user: User,
        var device: Device? = null, //nullable only for BC
        var application: Application,
        val space_id: Id<Space> = NullId(),  //nullable only for backward compatibility
        var location: Location? = null,
        val initiationId: String? = null,
        var turns: List<Turn> = emptyList(),
        val metrics: MutableList<Metric> = mutableListOf(),
        val properties: MutablePropertyMap = mutableMapOf(),
        val attributes: Attributes = Attributes(),
        val dialogueStack: DialogueStack = LinkedList()
) : TimeEntity<Session> {
    val isInitiated get() = initiationId != null
    val clientType get()  = attributes[DialogueModel.defaultNamespace]?.get("clientType")?.let {
        (it as Memory<String>).value
    } ?: "unknown"

    data class DialogueStackFrame(
            val id: String? = null, // nullable to be able to load older sessions
            val buildId: String,
            val args: PropertyMap,
            val nodeId: Int = 0,
            val name: String? = null
    )
}
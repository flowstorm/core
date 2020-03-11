package com.promethist.core.model

import org.litote.kmongo.Id
import java.io.Serializable
import java.util.*

open class Dialogue(
        var lastModified: Date = Date(),
        var modifiedBy: User? = null,
        var properties: MutableList<Property> = mutableListOf(),
        var state: State = State.Draft,
        var nodes: MutableSet<Node> = mutableSetOf()
    )
{
    private var nextNodeId: Int = 1

    enum class State { Draft, Public }

    data class Property(
            var name: String,
            var type: Type,
            var defaultValue: Serializable
    ) {
        enum class Type { Number, Text, ListOfNumbers, ListOfTexts, Boolean }
    }

    /* no need for JSON - KTS is the format
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = IntentNode::class, name = "Intent"),
    JsonSubTypes.Type(value = ResponseNode::class, name = "Response"),
    JsonSubTypes.Type(value = ResourceResponseNode::class, name = "ResourceResponse"),
    JsonSubTypes.Type(value = FunctionNode::class, name = "Function"),
    JsonSubTypes.Type(value = ExternalFunctionNode::class, name = "ExternalFunction"),
    JsonSubTypes.Type(value = SubdialogueNode::class, name = "Subdialogue")
)
*/
    abstract inner class Node(open val id: Int) {
        lateinit var nextNode: Node
        val attributes: MutableMap<String, Any> = mutableMapOf()

        override fun hashCode(): Int = id
        fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this@Revision")
    }

    inner class Intent(
            override val id: Int,
            val utterances: List<String>
    ): Node(id)

    open inner class Response(
            override val id: Int,
            open var texts: List<String>,
            open var isSsml: Boolean
    ): Node(id)

    inner class ResourceResponse(
            override val id: Int,
            override var texts: List<String>,
            override var isSsml: Boolean = false,
            val resource_id: Id<*>
    ): Response(id, texts, isSsml)

    inner class Function(
            override val id: Int,
            val function: (Function.(Context) -> Node)
    ): Node(id)

    // externals functions should be converted into functions during build
    inner class ExternalFunction(
            override val id: Int,
            var name: String
    ): Node(id)

    inner class Subdialogue(
            override val id: Int,
            var name: String
    ): Node(id)

    inner class StopDialogue(override val id: Int = nextNodeId++) : Node(id)

    inner class StopSession(override val id: Int = nextNodeId++) : Node(id)

    fun intent(id: Int = nextNodeId++, utterances: List<String>): Intent =
            Intent(id, utterances).apply { nodes.add(this) }

    fun response(id: Int = nextNodeId++, texts: List<String>, isSsml: Boolean = false): Response =
            Response(id, texts, isSsml).apply { nodes.add(this) }

    fun function(id: Int = nextNodeId++, function: (Function.(Context) -> Node)): Function =
            Function(id, function).apply { nodes.add(this) }
}

fun dialogue(init: (Dialogue.() -> Dialogue.Node)): Dialogue {
    val dialogue = Dialogue()
    init.invoke(dialogue)
    return dialogue
}

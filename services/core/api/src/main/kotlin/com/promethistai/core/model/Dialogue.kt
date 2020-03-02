package com.promethistai.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.io.Serializable
import java.util.*

data class Dialogue(
        var _id: Id<Dialogue>,
        var name: String,
        var versions: List<Revision>
) {

    // dialogue manager persists and runs just versions (while editor manages whole dialog models with all versions)
    data class Revision(
            var name: String, // helena stores "${dialogue.name}:$revision_index"
            var lastModified: Date,
            var modifiedBy: User,
            var properties: List<Property>,
            var state: State,
            var nodes: HashSet<Node>
    ) {
        enum class State { Draft, Public }

        fun getProperty(name: String) = properties.find { it.name == name }
        fun getSubdialogueNodes(): List<SubdialogueNode> = nodes.filterIsInstance<SubdialogueNode>()
        fun getFunctions(): List<*> { TODO() }
    }

    data class Property(
            var name: String,
            var type: Type,
            var defaultValue: Serializable
    ) {
        enum class Type { Number, Text, ListOfNumbers, ListOfTexts, Boolean }
    }

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
    abstract class Node(
            var _id: Id<Node> = newId(),
            open var name: String,
            var nextNodes: List<Id<Node>> = mutableListOf()
    ) {
        override fun hashCode(): Int = _id.hashCode()
    }

    data class IntentNode(
        override var name: String,
        var utterances: List<String>
    ): Node(name = name)

    abstract class ResponseNode(
            override var name: String,
            open var texts: List<String>,
            open var isSsml: Boolean
    ): Node(name = name)

    data class ResourceResponseNode(
            override var name: String,
            override var texts: List<String>,
            override var isSsml: Boolean = false,
            var resource_id: Id<*>
    ): ResponseNode(name = name, texts = texts, isSsml = isSsml)

    data class FunctionNode(
            override var name: String,
            var type: Function.Type,
            override var source: String
    ): Node(name = name), Function

    // externals functions should be converted into functions during build
    data class ExternalFunctionNode(
            override var name: String,
            var function_id: Id<*>
    ): Node(name = name)

    data class SubdialogueNode(
            override var name: String,
            var dialogue_id: Id<Dialogue>
    ): Node(name = name)

}
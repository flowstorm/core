package com.promethistai.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import java.io.Serializable
import java.util.*

open class DataObject: Hashtable<String, Serializable> {

    open class Deserializer<O>(val objectClass: Class<O>): JsonDeserializer<O>() where O : DataObject {

        open val skipFields = setOf<String>()

        override fun deserialize(parser: JsonParser?, ctx: DeserializationContext?): O {
            val mapper = ObjectMapper()
            val codec = parser!!.codec
            val tree = codec.readTree<JsonNode>(parser)
            val obj = objectClass.newInstance()
            for (field in tree.fields()) {
                if (skipFields.contains(field.key))
                    continue
                obj[field.key] = deserializeField(obj, tree, field, mapper, null)
            }
            return obj
        }

        open fun deserializeField(obj: DataObject, tree: JsonNode, field: Map.Entry<String, JsonNode>, mapper: ObjectMapper, type: String?): Serializable? {
            return deserializeNode(field.value, mapper, type)
        }

        open fun deserializeNode(node: JsonNode, mapper: ObjectMapper, type: String?): Serializable? {
            if (node is NullNode)
                return null
            if (node is ObjectNode)
                return mapper.convertValue(node, objectClass)
            if (node is ArrayNode) {
                val list = Vector<Serializable>()
                for (i in 0 until node.size())
                    list.add(deserializeNode(node.get(i), mapper, null)!!)
                return list
            }
            if ("LONG" == type || node is LongNode || node is IntNode || node is ShortNode)
                return node.asLong()
            if ("DOUBLE" == type || node is DoubleNode || node is FloatNode)
                return node.asDouble()
            if ("BOOLEAN" == type || node is BooleanNode)
                return node.asBoolean()

            return node.asText()

        }
    }

    constructor(): super()

    constructor(props: Map<String, Serializable>): this() {
        set(props)
    }

    fun set(props: Map<String, Serializable>): DataObject {
        for (prop in props)
            this[prop.key] = prop.value
        return this
    }



}
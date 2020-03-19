package com.promethist.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.ObjectUtil
import com.promethist.core.type.Dynamic
import java.util.*

data class Turn(
        var input: Input,
        var attributes: Dynamic = Dynamic(),
        val dialogueStack: LinkedList<DialogueStackFrame> = LinkedList(),
        val responseItems: MutableList<MessageItem> = mutableListOf()
) {
    data class DialogueStackFrame(val name: String, var nodeId: Int = 0)

    data class Input(val text: String, val classes: MutableList<Class> = mutableListOf(), val tokens: MutableList<Token> = mutableListOf()) {

        data class Class(val type: Type, val name: String) {
            enum class Type { Intent, Entity }
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes(
                JsonSubTypes.Type(value = Word::class, name = "Word"),
                JsonSubTypes.Type(value = Punctuation::class, name = "Punctuation")
        )
        open class Token(open val text: String)

        data class Word(override val text: String, val classes: MutableList<Class> = mutableListOf()) : Token(text)

        data class Punctuation(override val text: String) : Token(text)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = Input("I see a dog and a cat.", mutableListOf(Input.Class(Input.Class.Type.Intent, "-1")), mutableListOf(
                    Input.Word("i"),
                    Input.Word("see"),
                    Input.Word("dog", mutableListOf(Input.Class(Input.Class.Type.Entity, "animal"))),
                    Input.Word("and"),
                    Input.Word("cat", mutableListOf(Input.Class(Input.Class.Type.Entity, "animal"))),
                    Input.Punctuation(".")
            ))
            println(ObjectUtil.defaultMapper.writeValueAsString(input))
        }
    }
}

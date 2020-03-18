package com.promethist.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.core.type.Dynamic
import java.util.*

data class Turn(
        var input: Input,
        var attributes: Dynamic = Dynamic(),
        val dialogueStack: LinkedList<DialogueStackFrame> = LinkedList(),
        val responseItems: MutableList<MessageItem> = mutableListOf()
) {
    data class DialogueStackFrame(val name: String, var nodeId: Int = 0)

    data class Input(val message: String, var clazz: String, val tokens: MutableList<Token> = mutableListOf()) {

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes(
                JsonSubTypes.Type(value = Word::class, name = "Word"),
                JsonSubTypes.Type(value = Punctuation::class, name = "Punctuation")
        )
        open class Token(open val text: String)

        data class Word(override val text: String, val classes: MutableList<String> = mutableListOf()) : Token(text)

        data class Punctuation(override val text: String) : Token(text)
    }
}

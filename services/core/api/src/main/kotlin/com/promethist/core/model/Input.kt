package com.promethist.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.ObjectUtil
import java.util.ArrayList

data class Input(var text: String, val classes: MutableList<Class> = mutableListOf(), val tokens: MutableList<Token> = mutableListOf()) {

    data class Class(val type: Type, val name: String) {
        enum class Type { Intent, Entity }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
            JsonSubTypes.Type(value = Word::class, name = "Word"),
            JsonSubTypes.Type(value = Punctuation::class, name = "Punctuation")
    )
    open class Token(open val text: String)

    data class Word(override val text: String, val classes: MutableList<Class> = mutableListOf()) : Token(text) {
        fun isClass(type: Class.Type, name: String) = classes.any { it.type == type && it.name == name }
        fun isEntity(name: String) = isClass(Class.Type.Entity, name)
    }

    data class Punctuation(override val text: String) : Token(text)

    class WordList(words: List<Word>) : ArrayList<Word>() {
        init {
            addAll(words)
        }

        fun entities(name: String) = filter { it.classes.any { it.type == Class.Type.Entity && it.name == name } }
    }

    val words: WordList by lazy { WordList(tokens.filterIsInstance<Word>()) }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = Input("I see a dog and a cat.", mutableListOf(Class(Class.Type.Intent, "-1")),
                mutableListOf(
                    Word("i"),
                    Word("see"),
                    Word("dog", mutableListOf(Class(Class.Type.Entity, "animal"))),
                    Word("and"),
                    Word("cat", mutableListOf(Class(Class.Type.Entity, "animal"))),
                    Punctuation(".")
                )
            )
            println(input.words.entities("animal"))
            println(ObjectUtil.defaultMapper.writeValueAsString(input))
        }
    }
}
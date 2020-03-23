package com.promethist.core.nlp

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.ObjectUtil
import java.util.ArrayList

data class Input(var text: String, val classes: MutableList<Class> = mutableListOf(), val tokens: MutableList<Token> = mutableListOf()) {

    data class Class(val type: Type, val name: String, val score: Float = 1.0F) {
        enum class Type { Intent, Entity }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
            JsonSubTypes.Type(value = Word::class, name = "Word"),
            JsonSubTypes.Type(value = Punctuation::class, name = "Punctuation")
    )
    open class Token(open val text: String)

    data class Word(override val text: String, val classes: MutableList<Class> = mutableListOf()) : Token(text) {
        fun hasClass(type: Class.Type, name: String) = classes.any { it.type == type && it.name == name }
        fun isEntity(name: String) = hasClass(Class.Type.Entity, name)
    }

    data class Punctuation(override val text: String) : Token(text)

    class WordList(words: List<Word>) : ArrayList<Word>() {
        init {
            addAll(words)
        }

        fun entities(name: String) = filter { it.classes.any { it.type == Class.Type.Entity && it.name == name } }
    }

    val words: WordList by lazy { WordList(tokens.filterIsInstance<Word>()) }
    val entities: Map<String, List<String>> by lazy {
        val map = mutableMapOf<String, MutableList<String>>()
        words.forEach { word ->
            word.classes.forEach {
                if (it.type == Class.Type.Entity) {
                    val beginning = it.name.startsWith("B-")
                    val inside = it.name.startsWith("I-")
                    val name = if (beginning || inside) it.name.substring(2) else it.name
                    if (!map.containsKey(name))
                        map[name] = mutableListOf()
                    else if (inside) {
                        val last = map[name]!!.size - 1
                        map[name]!![last] = map[name]!![last] + " " + word.text
                    }
                    if (!inside)
                        map[name]!!.add(word.text)
                }
            }
        }
        map
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = Input("I see a dog, a red rose and a cat.", mutableListOf(Class(Class.Type.Intent, "-1")),
                    mutableListOf(
                            Word("i"),
                            Word("see"),
                            Word("dog", mutableListOf(Class(Class.Type.Entity, "animal"))),
                            Punctuation(","),
                            Word("red", mutableListOf(Class(Class.Type.Entity, "B-flower"))),
                            Word("rose", mutableListOf(Class(Class.Type.Entity, "I-flower"))),
                            Word("and"),
                            Word("cat", mutableListOf(Class(Class.Type.Entity, "animal"))),
                            Punctuation(".")
                    )
            )
            println(input.words.entities("animal"))
            println(input.entities)
            println(ObjectUtil.defaultMapper.writeValueAsString(input))
        }
    }
}
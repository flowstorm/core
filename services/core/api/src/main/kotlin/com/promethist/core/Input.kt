package com.promethist.core

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.ObjectUtil
import java.util.*

data class Input(
        val language: Locale = Locale.ENGLISH,
        var transcript: Transcript = Transcript(""),
        val alternatives: MutableList<Transcript> = mutableListOf(),
        val classes: MutableList<Class> = mutableListOf(),
        val tokens: MutableList<Token> = mutableListOf()
) {
    data class Transcript(val text: String, val confidence: Float = 1.0F)

    data class Class(val type: Type, val name: String, val score: Float = 1.0F) {
        enum class Type { Intent, Entity }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
            JsonSubTypes.Type(value = Word::class, name = "Word"),
            JsonSubTypes.Type(value = Punctuation::class, name = "Punctuation")
    )
    open class Token(open val text: String) {
        override fun equals(other: Any?): Boolean = text.equals(other)
        override fun hashCode(): Int = text.hashCode()
    }

    data class Word(override val text: String, val classes: MutableList<Class> = mutableListOf(), val startTime: Float = 0F, val endTime: Float = 0F) : Token(text) {
        fun hasClass(type: Class.Type, name: String) = classes.any { it.type == type && it.name == name }
        fun isEntity(name: String) = hasClass(Class.Type.Entity, name)
    }

    data class Punctuation(override val text: String) : Token(text)

    data class Entity(val className: String, var value: String)

    class WordList(words: List<Word>) : ArrayList<Word>() {
        init {
            addAll(words)
        }

        fun entities(name: String) = filter { it.classes.any { it.type == Class.Type.Entity && it.name == name } }
    }

    @get:JsonIgnore
    val words: WordList by lazy { WordList(tokens.filterIsInstance<Word>()) }

    @get:JsonIgnore
    val intents get() = classes.filter { it.type == Class.Type.Intent }

    @get:JsonIgnore
    val intent get() = intents.firstOrNull()?:error("No intent class recognized in input")

    @get:JsonIgnore
    val entityMap: Map<String, List<Entity>> by lazy {
        val map = mutableMapOf<String, MutableList<Entity>>()
        words.forEach { word ->
            word.classes.forEach {
                if (it.type == Class.Type.Entity) {
                    val beginning = it.name.startsWith("B-")
                    val inside = it.name.startsWith("I-")
                    val className = if (beginning || inside) it.name.substring(2) else it.name
                    if (!map.containsKey(className))
                        map[className] = mutableListOf()
                    else if (inside) {
                        val last = map[className]!!.size - 1
                        map[className]!![last].value = map[className]!![last].value + " " + word.text
                    }
                    if (!inside)
                        map[className]!!.add(Entity(className, word.text))
                }
            }
        }
        map
    }

    fun entities(className: String) = entityMap[className]?.map { it.value } ?: listOf()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val input = Input(
                    Locale.ENGLISH,
                    Transcript("I see a dog, a red rose and a cat."),
                    classes = mutableListOf(Class(Class.Type.Intent, "-1")),
                    tokens = mutableListOf(
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
            println(input.intent?.name)
            println(input.entityMap)
            println(input.entities("animal"))
            println(ObjectUtil.defaultMapper.writeValueAsString(input))
        }
    }
}
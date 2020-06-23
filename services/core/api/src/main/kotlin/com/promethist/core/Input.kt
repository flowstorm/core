package com.promethist.core

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.promethist.common.ObjectUtil
import java.time.*
import java.util.*

data class Input(
        val locale: Locale = Defaults.locale,
        val zoneId: ZoneId = Defaults.zoneId,
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
        override fun hashCode() = text.hashCode()
        override fun equals(other: Any?) = if (other is Word) text == other.text else super.equals(other)
    }

    data class Punctuation(override val text: String) : Token(text)

    data class Entity(val className: String, var value: String, var confidence: Float)

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
    val numbers: List<Number> get() {
        val numbers = mutableListOf<Number>()
        transcript.text.replace(Regex("(-?[\\d]+(\\.?,?\\d+)?)")) {
            val s = it.groupValues[1].replace(',', '.')
            numbers.add(if (s.indexOf('.') >= 0) s.toFloat() else s.toInt())
            s
        }
        return numbers
    }

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
                        map[className]!![last].value += " " + word.text
                        var length = map[className]!![last].value.split(" ").size
                        map[className]!![last].confidence += (it.score - map[className]!![last].confidence) / length
                    }
                    if (!inside)
                        map[className]!!.add(Entity(className, word.text, it.score))
                }
            }
        }
        map
    }

    fun entities(className: String) = entityMap[className]?.map { it.value } ?: listOf()
}
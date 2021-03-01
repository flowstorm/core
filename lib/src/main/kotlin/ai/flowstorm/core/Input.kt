package ai.flowstorm.core

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import ai.flowstorm.core.model.Sentiment
import ai.flowstorm.core.type.InputEntity
import ai.flowstorm.core.type.value.Value
import java.time.ZoneId
import java.util.*

data class Input(
        val locale: Locale = Defaults.locale,
        val zoneId: ZoneId = Defaults.zoneId,
        var transcript: Transcript = Transcript(""),
        val alternatives: MutableList<Transcript> = mutableListOf(),
        val classes: MutableList<Class> = mutableListOf(),
        val tokens: MutableList<Token> = mutableListOf()
) {
    data class Transcript(var text: String, val confidence: Float = 1.0F)

    data class Class(val type: Type, val name: String, val score: Float = 1.0F, val model_id: String = "") {
        enum class Type { Intent, Entity, Sentiment }
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

    class WordList(words: List<Word>) : ArrayList<Word>() {
        init {
            addAll(words)
        }

        fun entities(name: String) = filter { it.classes.any { it.type == Class.Type.Entity && it.name == name } }
    }

    @get:JsonIgnore
    val words get() = WordList(tokens.filterIsInstance<Word>())

    @get:JsonIgnore
    val intents get() = classes.filter { it.type == Class.Type.Intent }

    @get:JsonIgnore
    val intent get() = intents.firstOrNull() ?: error("No intent class recognized in input")

    @get:JsonIgnore
    val sentiments get() = classes.filter { it.type == Class.Type.Sentiment }

    @get:JsonIgnore
    val sentiment get() = sentiments.firstOrNull() ?: Class(Class.Type.Sentiment, Sentiment.UNKNOWN.name)

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

    var action: String? = null

    @get:JsonIgnore
    val entityMap: MutableMap<String, MutableList<InputEntity>> by lazy { InputEntity.fromAnnotation(words) }

    fun containsEntity(className: String) = entityMap.containsKey(className)

    inline fun <reified V : Value> containsEntity() = containsEntity(V::class.simpleName!!)

    fun entities(className: String) = entityMap[className] ?: listOf<InputEntity>()

    inline fun <reified V : Value> entities(): List<V> {
        val className = V::class.simpleName!!
        return if (entityMap.containsKey(className))
            entityMap[className]?.map { it.value as V } ?: listOf()
        else
            listOf()
    }

    fun entity(className: String) = entities(className).first()

    inline fun <reified V : Value> entity(): V = entities<V>().first()
}
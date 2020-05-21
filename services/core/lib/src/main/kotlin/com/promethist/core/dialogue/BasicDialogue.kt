package com.promethist.core.dialogue

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.language.English
import com.promethist.core.type.*
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.StringTokenizer

abstract class BasicDialogue : Dialogue() {

    enum class Article { None, Indefinite, Definite }

    companion object {

        const val LOW = 0
        const val MEDIUM = 1
        const val HIGH = 2

        val pass: Transition? = null
        @Deprecated("Use pass instead, toIntent will be removed")
        val toIntent = pass

        val now: DateTime get() = with (threadContext()) { DateTime.now(context.turn.input.zoneId) }
        val today get() = now.day
        val tomorrow get() = now + 1
        val yesterday get() = now - 1
        val DateTime.isToday get() = this isDay 0..0
        val DateTime.isTomorrow get() = this isDay 1..1
        val DateTime.isYesterday get() = this isDay -1..-1
        val DateTime.isHoliday get() = isWeekend // && TODO check holidays in context.turn.input.locale.country
        val DateTime.holidayName get() = null // && TODO check holidays in context.turn.input.locale.country
        val DateTime.monthName get() = English.months[month.value - 1] //TODO localize
        val DateTime.dayOfWeekName get() = English.weekDays[dayOfWeek.value - 1] //TODO localize
        infix fun DateTime.isDay(range: IntRange) =
                day(range.first.toLong()) >= today && today < day(range.last.toLong() + 1)
        infix fun DateTime.isDay(day: Int) = this isDay day..day
    }

    val location by turnAttribute(clientNamespace) { Location() }

    var turnSpeakingRate by turnAttribute(clientNamespace) { 1.0 }
    var sessionSpeakingRate by sessionAttribute(clientNamespace) { 1.0 }
    var userSpeakingRate by userAttribute(clientNamespace) { 1.0 }

    var turnSpeakingPitch by turnAttribute(clientNamespace) { 0.0 }
    var sessionSpeakingPitch by sessionAttribute(clientNamespace) { 0.0 }
    var userSpeakingPitch by userAttribute(clientNamespace) { 0.0 }

    var turnSpeakingVolumeGain by turnAttribute(clientNamespace) { 1.0 }
    var sessionSpeakingVolumeGain by sessionAttribute(clientNamespace) { 1.0 }
    var userSpeakingVolumeGain by userAttribute(clientNamespace) { 1.0 }

    inline fun <reified V: Any> turnAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Turn, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> sessionAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> userAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.User, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> communityAttribute(communityName: String, namespace: String? = null, noinline default: (Context.() -> V)) =
            CommunityAttributeDelegate(V::class, communityName, { namespace?:dialogueNameWithoutVersion }, default)

    inline fun <reified E: NamedEntity> turnEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> turnEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> turnEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    fun metricValue(metricSpec: String) = MetricDelegate(metricSpec)

    inline fun <reified T: Any> loader(path: String): Lazy<T> = lazy {
        val typeRef = object : TypeReference<T>() {}
        when {
            path.startsWith("file:///") ->
                FileInputStream(File(path.substring(7))).use {
                    ObjectUtil.defaultMapper.readValue<T>(it, typeRef)
                }
            path.startsWith("http") ->
                URL(path).openStream().use {
                    ObjectUtil.defaultMapper.readValue<T>(it, typeRef)
                }
            path.startsWith("./") ->
                loader.loadObject(dialogueName + path.substring(1).substringBeforeLast(".json"), typeRef)
            else ->
                loader.loadObject(path.substringBeforeLast(".json"), typeRef)
        }
    }

    fun communityAttributes(communityName: String) = with (threadContext()) { context.communityResource.get(communityName)?.attributes ?: Dynamic.EMPTY }

    fun addResponseItem(vararg value: Any, image: String? = null, audio: String? = null, video: String? = null, repeatable: Boolean = true) = with (threadContext()) {
        context.turn.addResponseItem(enumerate(*value), image, audio, video, repeatable)
    }

    private inline fun unsupportedLanguage(): Nothing {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${language} of dialogue ${dialogueName}")
    }

    // subjective

    fun empty(subj: String) =
            when (language) {
                "en" -> "no"
                "de" -> "kein" //TODO male vs. female
                else -> unsupportedLanguage()
            } + " $subj"

    fun lemma(word: String) = word

    fun plural(input: String, count: Int = 2) =
        if (input.isBlank()) {
            input
        } else with (if (input.indexOf('+') > 0) input else "$input+") {
            split(" ").joinToString(" ") {
                val word = if (it[it.length - 1] == '+' || it[it.length - 1] == '?')
                    it.substring(0, it.length - 1)
                else
                    it
                if (count < 1 && it.endsWith('?')) {
                    ""
                } else if (count > 1 && it.endsWith('+')) {
                    when (language) {
                        "en" -> English.irregularPlurals.getOrElse(word) {
                            when {
                                word.endsWith("y") ->
                                    word.substring(0, word.length - 1) + "ies"
                                word.endsWith(listOf("s", "sh", "ch", "x", "z", "o")) ->
                                    word + "es"
                                else ->
                                    word + "s"
                            }
                        }
                        else -> unsupportedLanguage()
                    }
                } else {
                    word
                }
            }
        }

    fun plural(data: Collection<String>, count: Int = 2) = enumerate(data.map { plural(it, count) })

    fun article(subj: String, article: Article = Article.Indefinite) =
            when (language) {
                "en" -> when (article) {
                    Article.Indefinite -> (if (subj.startsWithVowel()) "an " else "a ") + subj
                    Article.Definite -> "the $subj"
                    else -> subj
                }
                else -> subj
            }

    fun definiteArticle(subj: String) = article(subj, Article.Definite)

    fun indent(value: Any?, separator: String = " ") = (value?.let { separator + describe(value) } ?: "")

    fun greeting(name: String? = null) = (
        if (now.hour >= 18 || now.hour < 3)
            mapOf(
                    "en" to "good evening",
                    "de" to "guten abend",
                    "cs" to "dobrý večer",
                    "fr" to "bonsoir"
            )[language] ?: unsupportedLanguage()
        else if (now.hour < 12)
            mapOf(
                    "en" to "good morning",
                    "de" to "guten morgen",
                    "cs" to "dobré ráno",
                    "fr" to "bonjour"
            )[language] ?: unsupportedLanguage()
        else
            mapOf(
                    "en" to "good afternoon",
                    "de" to "guten tag",
                    "cs" to "dobré odpoledne",
                    "fr" to "bonne après-midi"
            )[language] ?: unsupportedLanguage()
        ) + indent(name, ", ")

    fun farewell(name: String? = null) = (
            if (now.hour >= 21 || now.hour < 3)
                mapOf(
                        "en" to "good night",
                        "de" to "gute nacht",
                        "cs" to "dobrou noc",
                        "fr" to "bonne nuit"
                )[language] ?: unsupportedLanguage()
            else
                mapOf(
                        "en" to "good bye",
                        "de" to "auf wiedersehen",
                        "cs" to "nashledanou",
                        "fr" to "au revoir"
                )[language] ?: unsupportedLanguage()
            ) + indent(name, ", ")

    // descriptive

    fun describe(data: Map<String, Any>): String {
        val list = mutableListOf<String>()
        val isWord = when (language) {
            "en" -> "is"
            "de" -> "ist"
            "cs" -> "je"
            else -> unsupportedLanguage()
        }
        data.forEach {
            list.add("${it.key} $isWord " + describe(it.value))
        }
        return enumerate(list)
    }

    fun describe(data: Collection<String>) = enumerate(data)

    fun describe(data: Value<*>) = describe(data.value) + indent(describe(data.time, HIGH))

    fun describe(data: Any?, detail: Int = 0) =
        when (data) {
            is Location -> "latitude is ${data.latitude}, longitude is ${data.longitude}"
            is DateTime -> data.toString()
            is String -> data
            null -> "unknown"
            else -> data.toString()
        }

    // quantitative

    infix fun Number.of(subj: String) =
            if (this == 0)
                empty(plural(subj, 0))
            else
                describe(this) + " " + plural(subj, this.toInt())

    infix fun Array<*>.of(subj: String) = size of subj

    infix fun Map<*, *>.of(subj: String) = size of subj

    // enumerative

    infix fun Collection<String>.of(subj: String) = enumerate(this, subj)

    fun enumerate(vararg data: Any?, subjBlock: (Int) -> String, before: Boolean = false, conj: String = "", detail: Int = 0) =
            enumerate(data.asList().map { describe(it, detail) }, subjBlock, before, conj)

    fun enumerate(vararg data: Any?, subj: String = "", before: Boolean = false, conj: String = "", detail: Int = 0) =
            enumerate(data.asList().map { describe(it, detail) }, subj, before, conj)

    fun enumerate(data: Collection<String>, subjBlock: (Int) -> String, before: Boolean = false, conj: String = ""): String {
        val list = if (data is List<String>) data else data.toList()
        val subj = subjBlock(list.size)
        when {
            list.isEmpty() ->
                return empty(subj)
            list.size == 1 ->
                return (if (before && subj.isNotEmpty()) "$subj " else "") +
                        list.first() +
                        (if (!before && subj.isNotEmpty()) " $subj" else "")
            else -> {
                val op = if (conj == "")
                    mapOf("en" to "and", "de" to "und", "cs" to "a")[language] ?: unsupportedLanguage()
                else
                    conj
                val str = StringBuilder()
                if (before && subj.isNotEmpty())
                    str.append(subj).append(' ')
                for (i in list.indices) {
                    if (i > 0)
                        str.append(if (i == list.size - 1) ", $op " else ", ")
                    str.append(list[i])
                }
                if (!before && subj.isNotEmpty())
                    str.append(' ').append(subj)
                return str.toString()
            }
        }
    }

    fun enumerate(subjBlock: (Int) -> String, data: Collection<String>, conj: String = "") =
            enumerate(data, subjBlock, true, conj)

    fun enumerate(subj: String, data: Collection<String>, conj: String = "") =
            enumerate(data, subj, true, conj)
    
    fun enumerate(data: Collection<String>, subj: String = "", before: Boolean = false, conj: String = "") =
            enumerate(data, { plural(subj, data.size) }, before, conj)

    fun enumerate(data: Map<String, Number>): String = enumerate(mutableListOf<String>().apply {
        data.forEach { add(it.value of it.key) }
    })

    fun enumerate(data: Dynamic) = enumerate(mutableListOf<String>().apply {
        data.forEach {
            if (it.value is Number)
                add(it.value as Number of it.key)
        }
    })

    fun enumerate(vararg pairs: Pair<String, Number>) = enumerate(pairs.toMap())
}

fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

fun String.tokenize(): List<Input.Word> {
    val tokens = mutableListOf<Input.Word>()
    val tokenizer = StringTokenizer(this, " \t\n\r,.:;?![]'")
    while (tokenizer.hasMoreTokens()) {
        tokens.add(Input.Word(tokenizer.nextToken().toLowerCase()))
    }
    return tokens
}
package com.promethist.core.dialogue

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.core.Context
import com.promethist.core.Input
import com.promethist.core.language.English
import com.promethist.core.runtime.Api
import com.promethist.core.type.*
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.StringTokenizer
import kotlin.reflect.full.memberProperties

abstract class BasicDialogue : Dialogue() {

    enum class Article { None, Indefinite, Definite }

    companion object {

        const val LOW = 0
        const val MEDIUM = 1
        const val HIGH = 2

        val pass: Transition? = null
        @Deprecated("Use pass instead, toIntent will be removed")
        val toIntent = pass

        val now: DateTime get() = DateTime.now(codeRun.context.turn.input.zoneId)
        val today get() = now.date
        val tomorrow get() = today + 1.day
        val yesterday get() = today - 1.day
        val DateTime.isToday get() = this isDay 0..0
        val DateTime.isTomorrow get() = this isDay 1..1
        val DateTime.isYesterday get() = this isDay -1..-1
        val DateTime.isHoliday get() = isWeekend // && TODO check holidays in context.turn.input.locale.country
        val DateTime.holidayName get() = null // && TODO check holidays in context.turn.input.locale.country
        val DateTime.monthName get() = English.months[month.value - 1] //TODO localize
        val DateTime.dayOfWeekName get() = English.weekDays[dayOfWeek.value - 1] //TODO localize
        infix fun DateTime.isSecond(range: IntRange) = this >= now + range.first.second && this <= now + range.last.second
        infix fun DateTime.isMinute(range: IntRange) = this >= now + range.first.minute && this <= now + range.last.minute
        infix fun DateTime.isHour(range: IntRange) = this >= now + range.first.hour && this <= now + range.last.hour
        infix fun DateTime.isDay(range: IntRange) = date >= today + range.first.day && date <= today + range.last.day
        infix fun DateTime.isWeek(range: IntRange) = date >= today + range.first.week && date <= today + range.last.week
        infix fun DateTime.isMonth(range: IntRange) = date >= today + range.first.month && date <= today + range.last.month
        infix fun DateTime.isYear(range: IntRange) = date >= today + range.first.year && date <= today + range.last.year
        infix fun DateTime.isDay(day: Int) = this isDay day..day

        val api = Api
    }

    // client request attributes
    override var clientLocation by session(clientNamespace) { Location() }
    var clientType by session(clientNamespace) { "unknown" }
    var clientScreen by session(clientNamespace) { false }
    var clientTemperature by session(clientNamespace) { -273.15 }
    var clientAmbientLight by session(clientNamespace) { 0.0 }
    var clientSpatialMotion by session(clientNamespace) { 0.0 }

    // client response attributes
    var turnSpeakingRate by turn(clientNamespace) { 1.0 }
    var sessionSpeakingRate by session(clientNamespace) { 1.0 }
    var userSpeakingRate by user(clientNamespace) { 1.0 }

    var turnSpeakingPitch by turn(clientNamespace) { 0.0 }
    var sessionSpeakingPitch by session(clientNamespace) { 0.0 }
    var userSpeakingPitch by user(clientNamespace) { 0.0 }

    var turnSpeakingVolumeGain by turn(clientNamespace) { 1.0 }
    var sessionSpeakingVolumeGain by session(clientNamespace) { 1.0 }
    var userSpeakingVolumeGain by user(clientNamespace) { 1.0 }

    inline fun <reified V: Any> turn(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Turn, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> session(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> user(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.User, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> community(communityName: String, namespace: String? = null, noinline default: (Context.() -> V)) =
            CommunityAttributeDelegate(V::class, communityName, { namespace?:dialogueNameWithoutVersion }, default)

    inline fun sessionSequence(list: List<String>, namespace: String? = null, noinline nextValue: (SequenceAttribute<String, String>.() -> String?) = { nextRandom() }) =
            StringSequenceAttributeDelegate(list, ContextualAttributeDelegate.Scope.Session, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    inline fun userSequence(list: List<String>, namespace: String? = null, noinline nextValue: (SequenceAttribute<String, String>.() -> String?) = { nextRandom() }) =
            StringSequenceAttributeDelegate(list, ContextualAttributeDelegate.Scope.User, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    inline fun <reified E: NamedEntity> sessionSequence(entities: List<E>, namespace: String? = null, noinline nextValue: (SequenceAttribute<E, String>.() -> E?) = { nextRandom() }) =
            EntitySequenceAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    inline fun <reified E: NamedEntity> userSequence(entities: List<E>, namespace: String? = null, noinline nextValue: (SequenceAttribute<E, String>.() -> E?) = { nextRandom() }) =
            EntitySequenceAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    inline fun <reified E: NamedEntity> turnEntityList(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntityList(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntityList(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> turnEntitySet(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntitySet(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntitySet(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: Any> sessionMap(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: Any> turnMap(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: Any> userMap(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    // deprecated delegates with *Attribute suffix in name
    @Deprecated("Use turn instead", replaceWith = ReplaceWith("turn"))
    inline fun <reified V: Any> turnAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Turn, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    @Deprecated("Use session instead", replaceWith = ReplaceWith("session"))
    inline fun <reified V: Any> sessionAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    @Deprecated("Use user instead", replaceWith = ReplaceWith("user"))
    inline fun <reified V: Any> userAttribute(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.User, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    @Deprecated("Use community instead", replaceWith = ReplaceWith("community"))
    inline fun <reified V: Any> communityAttribute(communityName: String, namespace: String? = null, noinline default: (Context.() -> V)) =
            CommunityAttributeDelegate(V::class, communityName, { namespace?:dialogueNameWithoutVersion }, default)

    @Deprecated("Use sessionSequence instead", replaceWith = ReplaceWith("sessionSequence"))
    inline fun sessionSequenceAttribute(list: List<String>, namespace: String? = null, noinline nextValue: (SequenceAttribute<String, String>.() -> String?) = { nextRandom() }) =
            StringSequenceAttributeDelegate(list, ContextualAttributeDelegate.Scope.Session, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    @Deprecated("Use userSequence instead", replaceWith = ReplaceWith("userSequence"))
    inline fun userSequenceAttribute(list: List<String>, namespace: String? = null, noinline nextValue: (SequenceAttribute<String, String>.() -> String?) = { nextRandom() }) =
            StringSequenceAttributeDelegate(list, ContextualAttributeDelegate.Scope.User, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    @Deprecated("Use sessionSequence instead", replaceWith = ReplaceWith("sessionSequence"))
    inline fun <reified E: NamedEntity> sessionSequenceAttribute(entities: List<E>, namespace: String? = null, noinline nextValue: (SequenceAttribute<E, String>.() -> E?) = { nextRandom() }) =
            EntitySequenceAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    @Deprecated("Use userSequence instead", replaceWith = ReplaceWith("userSequence"))
    inline fun <reified E: NamedEntity> userSequenceAttribute(entities: List<E>, namespace: String? = null, noinline nextValue: (SequenceAttribute<E, String>.() -> E?) = { nextRandom() }) =
            EntitySequenceAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User, { namespace ?: dialogueNameWithoutVersion }, nextValue)

    @Deprecated("Use turnEntityList instead", replaceWith = ReplaceWith("turnEntityList"))
    inline fun <reified E: NamedEntity> turnEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use sessionEntityList instead", replaceWith = ReplaceWith("sessionEntityList"))
    inline fun <reified E: NamedEntity> sessionEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use userEntityList instead", replaceWith = ReplaceWith("userEntityList"))
    inline fun <reified E: NamedEntity> userEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use turnEntitySet instead", replaceWith = ReplaceWith("turnEntitySet"))
    inline fun <reified E: NamedEntity> turnEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use sessionEntitySet instead", replaceWith = ReplaceWith("sessionEntitySet"))
    inline fun <reified E: NamedEntity> sessionEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use userEntitySet instead", replaceWith = ReplaceWith("userEntitySet"))
    inline fun <reified E: NamedEntity> userEntitySetAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntitySetAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use sessionMap instead", replaceWith = ReplaceWith("sessionMap"))
    inline fun <reified E: Any> sessionMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use turnMap instead", replaceWith = ReplaceWith("turnMap"))
    inline fun <reified E: Any> turnMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    @Deprecated("Use userMap instead", replaceWith = ReplaceWith("userMap"))
    inline fun <reified E: Any> userMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            MapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

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

    fun communityAttributes(communityName: String) =
            codeRun.context.communityResource.get(communityName)?.attributes ?: Dynamic.EMPTY

    fun addResponseItem(vararg value: Any, image: String? = null, audio: String? = null, video: String? = null, repeatable: Boolean = true) =
            codeRun.context.turn.addResponseItem(enumerate(*value).replace(Regex("#([\\w\\.\\d]+)")) {
                var obj: Any? = this
                var point = false
                for (name in it.groupValues[1].split(".")) {
                    if (name.isBlank()) {
                        point = true
                        break
                    } else {
                        val prop = obj!!.javaClass.kotlin.memberProperties.firstOrNull { it.name == name }
                        obj = prop?.call(obj)
                        if (obj == null)
                            break
                    }
                }
                describe(obj) + (if (point) "." else "")
            }, image, audio, video, repeatable)

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

    fun plural(data: Collection<String>, count: Int = 2) = data.map { plural(it, count) }

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

    fun describe(data: Memory<*>) = describe(data.value) + indent(describe(data.time, HIGH))

    fun describe(data: Any?, detail: Int = 0) =
        when (data) {
            is Location -> "latitude is ${data.latitude}, longitude is ${data.longitude}"
            is DateTime -> data.toString()
            is String -> data
            null -> "undefined"
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

    fun enumerate(data: Collection<Number>, before: Boolean = false, conj: String = "") =
            enumerate(data.map { describe(it) }, "", before, conj)

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
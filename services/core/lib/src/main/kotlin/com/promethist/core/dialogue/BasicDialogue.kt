package com.promethist.core.dialogue

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil
import com.promethist.core.Context
import com.promethist.core.dialogue.attribute.*
import com.promethist.core.dialogue.metric.MetricDelegate
import com.promethist.core.model.ClientCommand
import com.promethist.core.model.enumContains
import com.promethist.core.runtime.Api
import com.promethist.core.type.*
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.time.Duration
import kotlin.random.Random
import kotlin.reflect.full.memberProperties

abstract class BasicDialogue : Dialogue() {

    companion object {

        const val LOW = 0
        const val MEDIUM = 1
        const val HIGH = 2

        val pass: Transition? = null

        @Deprecated("Use pass instead, toIntent will be removed")
        val toIntent = pass
        val api = Api()

        private val expander = ExampleExpander()
    }

    val now: DateTime get() = DateTime.now(run.context.turn.input.zoneId)
    val today get() = now.date
    val tomorrow get() = today + 1.day
    val yesterday get() = today - 1.day
    val DateTime.isToday get() = this isDay 0..0
    val DateTime.isTomorrow get() = this isDay 1..1
    val DateTime.isYesterday get() = this isDay -1..-1
    val DateTime.isHoliday get() = isWeekend // && TODO check holidays in context.turn.input.locale.country
    val DateTime.holidayName get() = null // && TODO check holidays in context.turn.input.locale.country
    val DateTime.isPast get() = Duration.between(this, now).isNegative
    infix fun DateTime.isDay(range: IntRange) =
            this + range.first.day <= today && today < this + range.last.day + 1.day
    infix fun DateTime.isDay(day: Int) = this isDay day..day

    override var clientLocation by session(defaultNamespace) { Location() }
    var clientType by session(defaultNamespace) { "unknown" }
    var clientScreen by session(defaultNamespace) { false }
    var clientTemperature by session(defaultNamespace) { -273.15 }
    var clientAmbientLight by session(defaultNamespace) { 0.0 }
    var clientSpatialMotion by session(defaultNamespace) { 0.0 }

    var nickname by user(defaultNamespace, true) { user.nickname }
    var gender by user(defaultNamespace) { "" }

    var dynamic by turn { Dynamic.EMPTY }

    var turnSpeakingRate by turn(defaultNamespace) { 1.0 }
    var sessionSpeakingRate by session(defaultNamespace) { 1.0 }
    var userSpeakingRate by user(defaultNamespace) { 1.0 }

    var turnSpeakingPitch by turn(defaultNamespace) { 0.0 }
    var sessionSpeakingPitch by session(defaultNamespace) { 0.0 }
    var userSpeakingPitch by user(defaultNamespace) { 0.0 }

    var turnSpeakingVolumeGain by turn(defaultNamespace) { 1.0 }
    var sessionSpeakingVolumeGain by session(defaultNamespace) { 1.0 }
    var userSpeakingVolumeGain by user(defaultNamespace) { 1.0 }

    inline fun <reified V: Any> temp(noinline default: (Context.() -> V)? = null) = TempAttributeDelegate(default)

    inline fun <reified V: Any> turn(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Turn, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> session(namespace: String? = null, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> client(noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { defaultNamespace }, default)

    inline fun <reified V: Any> user(namespace: String? = null, localize: Boolean = false, noinline default: (Context.() -> V)) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.User, V::class, {
                (namespace ?: dialogueNameWithoutVersion) + (if (localize) "/$language" else "")
            }, default)

    inline fun <reified V: Any> community(communityName: String, namespace: String? = null, localize: Boolean = false, noinline default: (Context.() -> V)) =
            CommunityAttributeDelegate(V::class, communityName, {
                (namespace ?: dialogueNameWithoutVersion) + (if (localize) "/$language" else "")
            }, default)

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

    fun metric(metricSpec: String) = MetricDelegate(metricSpec)

    @Deprecated("Use metric instead", replaceWith = ReplaceWith("metric"))
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
            run.context.communityResource.get(communityName)?.attributes ?: Dynamic.EMPTY

    fun addResponseItem(text: String?, image: String? = null, audio: String? = null, video: String? = null, repeatable: Boolean = true) =
            run.context.turn.addResponseItem(text?.let { evaluateTextTemplate(it) }, image, audio, video, repeatable, voice)

    /**
     * evaluate # in response text
     */
    open fun evaluateTextTemplate(text: String) = expander.expand(text).run {
        enumerate(this[Random.nextInt(size)]).
        replace(Regex("#([\\w\\.\\d]+)")) {
            if (enumContains<ClientCommand>(it.groupValues[1])) {
                "#" + it.groupValues[1]
            } else {
                var obj: Any? = this@BasicDialogue
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
            }
        }
    }

    inline fun unsupportedLanguage(): Nothing {
        val stackTraceElement = Thread.currentThread().stackTrace[1]
        throw error("${stackTraceElement.className}.${stackTraceElement.methodName} does not support language ${language} of dialogue ${dialogueName}")
    }

    fun indent(value: Any?, separator: String = " ") = (value?.let { separator + describe(value) } ?: "")

    infix fun Number.of(subj: String) =
            if (this == 0)
                empty(plural(subj, 0))
            else
                describe(this) + " " + plural(subj, this.toInt())

    infix fun Array<*>.of(subj: String) = size of subj

    infix fun Map<*, *>.of(subj: String) = size of subj

    infix fun Collection<String>.of(subj: String) = enumerate(this, subj)
}

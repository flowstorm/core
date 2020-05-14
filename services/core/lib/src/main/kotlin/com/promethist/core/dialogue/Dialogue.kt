package com.promethist.core.dialogue

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.core.Context
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.core.runtime.Loader
import com.promethist.core.type.Location
import com.promethist.core.type.NamedEntity
import com.promethist.core.type.PropertyMap
import java.io.File
import java.io.FileInputStream
import java.net.URL
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

abstract class Dialogue {

    val clientNamespace = "client"

    //dialogue config - must/may be overrided
    abstract val dialogueName: String
    open val buildId: String = "unknown" // used for generated classes, others are unknown
    open val language = "en"

    //runtime dependencies
    lateinit var loader: Loader
    val logger get() = threadContext().context.logger

    val nodes: MutableSet<Node> = mutableSetOf()
    var nextId: Int = 0
    var start = StartDialogue(nextId--)
    var goBack = GoBack(Int.MAX_VALUE)

    @Deprecated("Use `goBack` instead of `stop`")
    var stop = goBack
    var stopSession = StopSession(Int.MAX_VALUE - 1)
    var repeat = Repeat(Int.MAX_VALUE - 2)

    abstract inner class Node(open val id: Int) {
        val dialogue get() = this@Dialogue
        init { nodes.add(this) }

        override fun hashCode(): Int = id

        override fun toString(): String = "${javaClass.simpleName}" + (if (isSingleton) "" else "#$id")

        val isSingleton = this is StartDialogue || this is StopDialogue || this is StopSession || this is Repeat
    }

    abstract inner class TransitNode(override val id: Int): Node(id) {
        lateinit var next: Node
        fun isNextInitialized() = ::next.isInitialized
        override fun toString(): String = "${javaClass.simpleName}(id=$id, next=$next)"
    }

    data class Transition(val node: Node)

    inner class UserInput(
            override val id: Int,
            var skipGlobalIntents: Boolean,
            val intents: Array<Intent>,
            val lambda: (Context.(UserInput) -> Transition?)
    ): Node(id) {

        constructor(id: Int, intents: Array<Intent>, lambda: (Context.(UserInput) -> Transition?)) :
                this(id, false, intents, lambda)

        constructor(intents: Array<Intent>, lambda: (Context.(UserInput) -> Transition?)) :
                this(nextId--, false, intents, lambda)

        fun process(context: Context): Transition? =
                threadContext(context, this) { lambda(context, this) } as Transition?
    }

    open inner class Intent(
            override val id: Int,
            open val name: String,
            open val threshold: Float,
            vararg utterance: String
    ): TransitNode(id) {
        val utterances = utterance

        constructor(id: Int, name: String, vararg utterance: String) : this(id, name, 0.0F, *utterance)
        constructor(name: String, vararg utterance: String) : this(nextId--, name, *utterance)
    }

    inner class GlobalIntent(
             override val id: Int,
             override val name: String,
             override val threshold: Float,
             vararg utterance: String
    ): Intent(id, name, threshold, *utterance) {
        constructor(id: Int, name: String, vararg utterance: String) : this(id, name, 0.0F, *utterance)
    }

    open inner class Response(
            override val id: Int,
            val isRepeatable: Boolean = true,
            val image: String? = null,
            val audio: String? = null,
            val video: String? = null,
            vararg text: (Context.(Response) -> String)
    ): TransitNode(id) {
        val texts = text

        constructor(id: Int, isRepeatable: Boolean, vararg text: (Context.(Response) -> String)) : this(id, isRepeatable,null, null, null, *text)

        constructor(id: Int, vararg text: (Context.(Response) -> String)) : this(id, true, *text)

        constructor(vararg text: (Context.(Response) -> String)) : this(nextId--, true, *text)

        fun getText(context: Context, index: Int = -1) = threadContext(context,  this) {
            texts[if (index < 0) Random.nextInt(texts.size) else index](context, this)
        } as String
    }

    @Deprecated("Use goBack node instead.")
    inner class Repeat(override val id: Int): Node(id)

    inner class Function(
            override val id: Int,
            val lambda: (Context.(Function) -> Transition)
    ): Node(id) {
        constructor(lambda: (Context.(Function) -> Transition)) : this(nextId--, lambda)
        fun exec(context: Context): Transition =
                threadContext(context, this) { lambda(context, this) } as Transition
    }

    inner class SubDialogue(
            override val id: Int,
            val name: String,
            val lambda: Context.(SubDialogue) -> PropertyMap): TransitNode(id) {

        fun getConstructorArgs(context: Context): PropertyMap =
                threadContext(context, this) { lambda(context, this) } as PropertyMap

        fun create(vararg args: Pair<String, Any>): PropertyMap = args.toMap()
    }

    inner class StartDialogue(override val id: Int) : TransitNode(id)

    open inner class GoBack(override val id: Int, val repeat: Boolean = false) : Node(id)

    @Deprecated("Use node GoBack instead of StopDialogue")
    inner class StopDialogue(override val id: Int) : GoBack(id)

    inner class StopSession(override val id: Int) : Node(id)

    fun metricValue(metricSpec:String) = MetricDelegate(metricSpec)

    inline fun <reified V: Any> turnAttribute(namespace: String? = null, noinline default: (Context.() -> V)? = null) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Turn, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> sessionAttribute(namespace: String? = null, noinline default: (Context.() -> V)? = null) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.Session, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> userAttribute(namespace: String? = null, noinline default: (Context.() -> V)? = null) =
            ContextualAttributeDelegate(ContextualAttributeDelegate.Scope.User, V::class, { namespace ?: dialogueNameWithoutVersion }, default)

    inline fun <reified V: Any> communityAttribute(communityName: String, namespace: String? = null, noinline default: (Context.() -> V)? = null) =
            CommunityAttributeDelegate(V::class, communityName, { namespace?:dialogueNameWithoutVersion }, default)

    inline fun <reified E: NamedEntity> turnEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntityListAttribute(entities: Collection<E>, namespace: String? = null) =
            NamedEntityListAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> turnEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Turn) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> sessionEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.Session) { namespace ?: dialogueNameWithoutVersion }

    inline fun <reified E: NamedEntity> userEntityMapAttribute(entities: Map<String, E>, namespace: String? = null) =
            EntityMapAttributeDelegate(entities, ContextualAttributeDelegate.Scope.User) { namespace ?: dialogueNameWithoutVersion }

    val dialogueNameWithoutVersion get() = dialogueName.substringBeforeLast("/")

    @Deprecated("Use dialogueName instead", ReplaceWith("dialogueName"))
    val name get() = dialogueName

    @Deprecated("Use dialogueNameWithoutVersion instead", ReplaceWith("dialogueNameWithoutVersion"))
    val nameWithoutVersion get() = dialogueNameWithoutVersion

    val version get() = dialogueName.substringAfterLast("/").toInt()

    val intents: List<Intent> get() = nodes.filterIsInstance<Intent>()

    val globalIntents: List<GlobalIntent> get() = nodes.filterIsInstance<GlobalIntent>()

    val userInputs: List<UserInput> get() = nodes.filterIsInstance<UserInput>()

    val responses: List<Response> get() = nodes.filterIsInstance<Response>()

    val functions: List<Function> get() = nodes.filterIsInstance<Function>()

    val subDialogues: List<SubDialogue> get() = nodes.filterIsInstance<SubDialogue>()

    fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this")

    fun intentNode(id: Int) = intents.find { it.id == id } ?: error("Intent $id not found in $this")

    // implicit attributes
    val location by turnAttribute<Location>(clientNamespace)

    var turnSpeakingRate by turnAttribute(clientNamespace) { 1.0 }
    var sessionSpeakingRate by sessionAttribute(clientNamespace) { 1.0 }
    var userSpeakingRate by userAttribute(clientNamespace) { 1.0 }

    var turnSpeakingPitch by turnAttribute(clientNamespace) { 0.0 }
    var sessionSpeakingPitch by sessionAttribute(clientNamespace) { 0.0 }
    var userSpeakingPitch by userAttribute(clientNamespace) { 0.0 }

    var turnSpeakingVolumeGain by turnAttribute(clientNamespace) { 1.0 }
    var sessionSpeakingVolumeGain by sessionAttribute(clientNamespace) { 1.0 }
    var userSpeakingVolumeGain by userAttribute(clientNamespace) { 1.0 }

    val nodeMap: Map<String, Node> by lazy {
        javaClass.kotlin.members.filter {
            it is KProperty && it.returnType.isSubtypeOf(Node::class.createType())
        }.map { it.name to it.call(this) as Node }.toMap()
    }

    fun validate() {
        for (node in nodes) {
            val name by lazy { "${this::class.qualifiedName}: ${node::class.simpleName}(${node.id})" }
            if (node is TransitNode && !node.isNextInitialized())
                error("$name missing next node")
            if (node is UserInput && node.intents.isEmpty())
                error("$name missing intents")
        }
    }

    fun describe(): String {
        val sb = StringBuilder()
        nodeMap.forEach {
            sb.append(it.key).append(" = ").appendln(it.value)
        }
        return sb.toString()
    }

    inline fun <reified T: Any> loader(path: String): Lazy<T> = lazy {
        val typeRef = object : TypeReference<T>() {}
        when {
            path.startsWith("file:///") ->
                FileInputStream(File(path.substring(7))).use {
                    mapper.readValue<T>(it, typeRef)
                }
            path.startsWith("http") ->
                URL(path).openStream().use {
                    mapper.readValue<T>(it, typeRef)
                }
            path.startsWith("./") ->
                loader.loadObject(dialogueName + path.substring(1).substringBeforeLast(".json"), typeRef)
            else ->
                loader.loadObject(path.substringBeforeLast(".json"), typeRef)
        }
    }

    class ThreadContext(val dialogue: Dialogue, val context: Context)

    class DialogueScriptException(node: Node, cause: Throwable) : Throwable("DialogueScript failed at ${node.dialogue.dialogueName}#${node.id}", cause)

    companion object {

        private val threadContext = ThreadLocal<ThreadContext>()

        fun threadContext() = threadContext.get() ?: error("out of thread context")

        fun threadContext(context: Context, node: Node, block: () -> Any?): Any? =
                try {
                    threadContext.set(ThreadContext(node.dialogue, context))
                    block()
                } catch (e: Throwable) {
                    throw DialogueScriptException(node, e)
                } finally {
                    threadContext.remove()
                }
    }
}

package com.promethist.core

import com.fasterxml.jackson.core.type.TypeReference
import com.promethist.common.ObjectUtil.defaultMapper as mapper
import com.promethist.core.runtime.Loader
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.net.URL
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

abstract class Dialogue {

    //dialogue config - must/may be overrided
    abstract val name: String
    open val language = "en"

    //runtime dependencies
    lateinit var loader: Loader
    lateinit var logger: Logger

    val nodes: MutableSet<Node> = mutableSetOf()
    var nextId: Int = 0
    var start = StartDialogue(nextId--)
    var stop = StopDialogue(Int.MAX_VALUE)
    var stopSession = StopSession(Int.MAX_VALUE - 1)
    var repeat = Repeat(Int.MAX_VALUE - 2)

    abstract inner class Node(open val id: Int) {

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

        fun process(context: Context): Transition? =
                withCurrentContext(context) { lambda(context, this) } as Transition?
    }

    open inner class Intent(
            override val id: Int,
            open val name: String,
            vararg utterance: String
    ): TransitNode(id) {
        val utterances = utterance
    }

    inner class GlobalIntent(
             override val id: Int,
             override val name: String,
             vararg utterance: String
    ): Intent(id, name, *utterance)

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

        fun getText(context: Context, index: Int = -1) = withCurrentContext(context) {
            texts[if (index < 0) Random.nextInt(texts.size) else index](context, this)
        } as String
    }

    inner class Repeat(override val id: Int): Node(id)

    inner class Function(
            override val id: Int,
            val lambda: (Context.(Function) -> Transition)
    ): Node(id) {
        fun exec(context: Context): Transition =
                withCurrentContext(context) { lambda(context, this) } as Transition
    }

    inner class SubDialogue(
            override val id: Int,
            val name: String,
            val lambda: (Context.(SubDialogue) -> Dialogue)): TransitNode(id) {

        fun createDialogue(context: Context): Dialogue =
                withCurrentContext(context) { lambda(context, this) } as Dialogue

        fun create(vararg arg: Any) =
                loader.newObject<Dialogue>("$name/model", *arg).apply { loader = this@Dialogue.loader }

    }

    inner class StartDialogue(override val id: Int) : TransitNode(id)

    inner class StopDialogue(override val id: Int) : Node(id)

    inner class StopSession(override val id: Int) : Node(id)

    inline fun <reified V: Any> turnAttribute(namespace: String = nameWithoutVersion, noinline default: (() -> V)? = null) =
            AttributeDelegate(DialogueScript.AttributeScope.Turn, V::class, namespace, default)

    inline fun <reified V: Any> sessionAttribute(namespace: String = nameWithoutVersion, noinline default: (() -> V)? = null) =
            AttributeDelegate(DialogueScript.AttributeScope.Session, V::class, namespace, default)

    inline fun <reified V: Any> profileAttribute(namespace: String = nameWithoutVersion, noinline default: (() -> V)? = null) =
            AttributeDelegate(DialogueScript.AttributeScope.Profile, V::class, namespace, default)

    val nameWithoutVersion get() = name.substringBeforeLast("/")

    val intents: List<Intent> get() = nodes.filterIsInstance<Intent>()

    val globalIntents: List<GlobalIntent> get() = nodes.filterIsInstance<GlobalIntent>()

    val userInputs: List<UserInput> get() = nodes.filterIsInstance<UserInput>()

    val responses: List<Response> get() = nodes.filterIsInstance<Response>()

    val functions: List<Function> get() = nodes.filterIsInstance<Function>()

    val subDialogues: List<SubDialogue> get() = nodes.filterIsInstance<SubDialogue>()

    fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this")

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

    fun <T: Any> loader(path: String): Lazy<T> = lazy {
        when {
            path.startsWith("file:///") ->
                FileInputStream(File(path.substring(7))).use {
                    mapper.readValue<T>(it, object : TypeReference<T>() {})
                }
            path.startsWith("http") ->
                URL(path).openStream().use {
                    mapper.readValue<T>(it, object : TypeReference<T>() {})
            }
            path.startsWith("./") ->
                loader.loadObject<T>(name + path.substring(1).substringBeforeLast(".json"))
            else ->
                loader.loadObject<T>(path.substringBeforeLast(".json"))
        }
    }

    companion object: DialogueScript()
}

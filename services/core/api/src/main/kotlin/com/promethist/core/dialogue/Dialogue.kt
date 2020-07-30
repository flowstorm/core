package com.promethist.core.dialogue

import com.promethist.core.Context
import com.promethist.core.model.TtsConfig
import com.promethist.core.model.Voice
import com.promethist.core.runtime.Loader
import com.promethist.core.type.Location
import com.promethist.core.type.PropertyMap
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

abstract class Dialogue {

    //dialogue config - must/may be overrided
    abstract val dialogueName: String
    open val buildId: String = "unknown" // used for generated classes, others are unknown
    open val language get() = TtsConfig.forVoice(voice)?.locale.language ?: error("unknown voice")

    open val voice = Voice.Grace
    val locale by lazy { Locale(language) }

    abstract val clientLocation: Location?

    //runtime dependencies
    lateinit var loader: Loader
    val logger get() = run.context.logger

    val nodes: MutableSet<Node> = mutableSetOf()
    var nextId: Int = 0
    var start = StartDialogue(nextId--)
    var goBack = GoBack(Int.MAX_VALUE)

    var stop = StopDialogue(Int.MAX_VALUE - 3)
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
            val commands: Array<Command>,
            val lambda: (Context.(UserInput) -> Transition?)
    ): Node(id) {

        constructor(id: Int, intents: Array<Intent>, commands: Array<Command>, lambda: (Context.(UserInput) -> Transition?)) :
                this(id, false, intents, commands, lambda)

        constructor(intents: Array<Intent>, commands: Array<Command>, lambda: (Context.(UserInput) -> Transition?)) :
                this(nextId--, false, intents, commands, lambda)

        fun process(context: Context): Transition? {
            val transition = run(context, this) { lambda(context, this) } as Transition?
            if (transition == null && intents.isEmpty() && commands.isEmpty()) throw DialogueScriptException(this, Exception("Can not pass processing to pipeline, there are no intents or commands following the user input node."))

            return transition
        }
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

    open inner class Command(
            override val id: Int,
            open val name: String,
            open val command: String
    ): TransitNode(id) {
        constructor(name: String, command: String) : this(nextId--, name, command)
    }

    inner class GlobalCommand(
            override val id: Int,
            override val name: String,
            override val command: String
    ): Command(id, name, command) {
        constructor(name: String, command: String) : this(nextId--, name, command)
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

        fun getText(context: Context, index: Int = -1) = run(context, this) {
            if (texts.isNotEmpty()) texts[if (index < 0) Random.nextInt(texts.size) else index](context, this) else null
        } as String?
    }

    @Deprecated("Use goBack node instead.")
    inner class Repeat(override val id: Int): Node(id)

    inner class Function(
            override val id: Int,
            val lambda: (Context.(Function) -> Transition)
    ): Node(id) {
        constructor(lambda: (Context.(Function) -> Transition)) : this(nextId--, lambda)
        fun exec(context: Context): Transition =
                run(context, this) { lambda(context, this) } as Transition
    }

    inner class SubDialogue(
            override val id: Int,
            val name: String,
            val lambda: Context.(SubDialogue) -> PropertyMap): TransitNode(id) {

        fun getConstructorArgs(context: Context): PropertyMap =
                run(context, this) { lambda(context, this) } as PropertyMap

        fun create(vararg args: Pair<String, Any>): PropertyMap = args.toMap()
    }

    inner class StartDialogue(override val id: Int) : TransitNode(id)

    open inner class GoBack(override val id: Int, val repeat: Boolean = false) : Node(id)

    inner class StopDialogue(override val id: Int) : Node(id)

    inner class StopSession(override val id: Int) : Node(id)

    val dialogueNameWithoutVersion get() = dialogueName.substringBeforeLast("/")

    @Deprecated("Use dialogueName instead", ReplaceWith("dialogueName"))
    open val name get() = dialogueName

    @Deprecated("Use dialogueNameWithoutVersion instead", ReplaceWith("dialogueNameWithoutVersion"))
    val nameWithoutVersion get() = dialogueNameWithoutVersion

    val version get() = dialogueName.substringAfterLast("/").toInt()

    val intents: List<Intent> get() = nodes.filterIsInstance<Intent>()

    val globalIntents: List<GlobalIntent> get() = nodes.filterIsInstance<GlobalIntent>()

    val commands: List<Command> get() = nodes.filterIsInstance<Command>()

    val globalCommands: List<GlobalCommand> get() = nodes.filterIsInstance<GlobalCommand>()

    val userInputs: List<UserInput> get() = nodes.filterIsInstance<UserInput>()

    val responses: List<Response> get() = nodes.filterIsInstance<Response>()

    val functions: List<Function> get() = nodes.filterIsInstance<Function>()

    val subDialogues: List<SubDialogue> get() = nodes.filterIsInstance<SubDialogue>()

    val context get() = run.context

    fun inContext(block: Context.() -> Any) = block(context)

    fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this")

    fun intentNode(id: Int) = intents.find { it.id == id } ?: error("Intent $id not found in $this")

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
        }
    }

    fun describe(): String {
        val sb = StringBuilder()
        nodeMap.forEach {
            sb.append(it.key).append(" = ").appendln(it.value)
        }
        return sb.toString()
    }

    class Run(val node: Node, val context: Context)

    class DialogueScriptException(node: Node, cause: Throwable) : Throwable("DialogueScript failed at ${node.dialogue.dialogueName}#${node.id}", cause)

    companion object {

        const val GENERATED_USER_INPUT_ID = 10000

        val defaultNamespace: String = "_default"
        @Deprecated("Use `defaultNamespace` instead of `clientNamespace`")
        val clientNamespace = defaultNamespace

        private val _run = ThreadLocal<Run>()

        val isRunning get() = (_run.get() != null)

        val run get() = _run.get() ?: error("dialogue code not running")

        fun run(context: Context, node: Node, block: () -> Any?): Any? =
            try {
                _run.set(Run(node, context))
                block()
            } catch (e: Throwable) {
                throw DialogueScriptException(node, e)
            } finally {
                _run.remove()
            }
    }
}

package org.promethist.core.dialogue

import org.promethist.core.Context
import org.promethist.core.ExpectedPhrase
import org.promethist.core.model.DialogueModel
import org.promethist.core.model.SttConfig
import org.promethist.core.model.TtsConfig
import org.promethist.core.model.Voice
import org.promethist.core.runtime.Loader
import org.promethist.core.type.Location
import org.promethist.core.type.PropertyMap
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

abstract class AbstractDialogue : DialogueModel {

    //dialogue config - must/may be overrided
    open val buildId: String = "unknown" // used for generated classes, others are unknown
    open val language get() = TtsConfig.forVoice(voice)?.locale.language ?: error("unknown voice")
    open val sttMode: SttConfig.Mode? = null
    open val background: String? = null
    open val voice = Voice.Grace
    val locale by lazy { Locale(language) }

    abstract val clientLocation: Location

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

    abstract inner class Node(val id: Int) {
        val dialogue get() = this@AbstractDialogue
        val isSingleton by lazy { this is StartDialogue || this is StopDialogue || this is StopSession || this is Repeat }
        init { nodes.add(this) }
        override fun hashCode(): Int = id
        override fun equals(other: Any?) = if (other is Node) other.id == id && other.dialogue.dialogueId == dialogueId else false
        override fun toString(): String = stringName + (if (isSingleton) "" else "#$id")
        private val stringName: String get() =
            when(this) {
                is StartDialogue -> "Enter"
                is StopDialogue -> "Exit"
                is StopSession -> "End"
                else -> javaClass.simpleName ?: ""
            }
    }

    abstract inner class TransitNode(id: Int): Node(id) {
        lateinit var next: Node
        fun isNextInitialized() = ::next.isInitialized
    }

    data class Transition(val node: Node)

    inner class UserInput(
            id: Int,
            var skipGlobalIntents: Boolean,
            val sttMode: SttConfig.Mode? = null,
            val expectedPhrases: List<ExpectedPhrase>,
            val intents: Array<Intent>,
            val actions: Array<Action>,
            val lambda: (Context.(UserInput) -> Transition?)
    ): Node(id) {

        constructor(id: Int, skipGlobalIntents: Boolean, sttMode: SttConfig.Mode? = null, intents: Array<Intent>, actions: Array<Action>, lambda: (Context.(UserInput) -> Transition?)) :
                this(id, skipGlobalIntents, null, listOf(), intents, actions, lambda)

        constructor(id: Int, skipGlobalIntents: Boolean, intents: Array<Intent>, actions: Array<Action>, lambda: (Context.(UserInput) -> Transition?)) :
                this(id, skipGlobalIntents, null, intents, actions, lambda)

        constructor(id: Int, intents: Array<Intent>, actions: Array<Action>, lambda: (Context.(UserInput) -> Transition?)) :
                this(id, false, null, intents, actions, lambda)

        constructor(intents: Array<Intent>, actions: Array<Action>, lambda: (Context.(UserInput) -> Transition?)) :
                this(nextId--, false, null, intents, actions, lambda)

        fun process(context: Context): Transition? {
            val transition = run(context, this) { lambda(context, this) } as Transition?
            if (transition == null && intents.isEmpty() && actions.isEmpty()) throw DialogueScriptException(this, Exception("Can not pass processing to pipeline, there are no intents or actions following the user input node."))

            return transition
        }
    }

    open inner class Intent(
            id: Int,
            open val name: String,
            open val threshold: Float,
            open val entities: List<String>,
            vararg utterance: String
    ): TransitNode(id) {
        val utterances = utterance

        constructor(id: Int, name: String, threshold: Float, vararg utterance: String) : this(id, name, threshold, listOf(), *utterance)
        constructor(id: Int, name: String, vararg utterance: String) : this(id, name, 0.0F, *utterance)
        constructor(name: String, vararg utterance: String) : this(nextId--, name, *utterance)
    }

    inner class GlobalIntent(
             id: Int,
             override val name: String,
             override val threshold: Float,
             override val entities: List<String>,
             vararg utterance: String
    ): Intent(id, name, threshold, *utterance) {
        constructor(id: Int, name: String, threshold: Float, vararg utterance: String) : this(id, name, threshold, listOf(), *utterance)
        constructor(id: Int, name: String, vararg utterance: String) : this(id, name, 0.0F, *utterance)
    }

    open inner class Action(
            id: Int,
            open val name: String,
            open val action: String
    ): TransitNode(id) {
        constructor(name: String, action: String) : this(nextId--, name, action)
    }

    inner class GlobalAction(
            id: Int,
            override val name: String,
            override val action: String
    ): Action(id, name, action) {
        constructor(name: String, action: String) : this(nextId--, name, action)
    }

    open inner class Response(
            id: Int,
            val isRepeatable: Boolean = true,
            val background: String? = null,
            val image: String? = null,
            val audio: String? = null,
            val video: String? = null,
            val code: String? = null,
            vararg text: (Context.(Response) -> String)
    ): TransitNode(id) {
        val texts = text

        constructor(id: Int, isRepeatable: Boolean = true, background: String? = null, image: String? = null, audio: String? = null, video: String? = null, vararg text: (Context.(Response) -> String)) : this(id, isRepeatable, background, image, audio, video, null, *text)

        constructor(id: Int, isRepeatable: Boolean, background: String?, vararg text: (Context.(Response) -> String)) : this(id, isRepeatable, background, null, null, null, null, *text)

        constructor(id: Int, isRepeatable: Boolean, vararg text: (Context.(Response) -> String)) : this(id, isRepeatable, null, null, null, null, null, *text)

        constructor(id: Int, vararg text: (Context.(Response) -> String)) : this(id, true, *text)

        constructor(vararg text: (Context.(Response) -> String)) : this(nextId--, true, *text)

        fun getText(context: Context, index: Int = -1) = run(context, this) {
            if (texts.isNotEmpty()) texts[if (index < 0) Random.nextInt(texts.size) else index](context, this) else null
        } as String?
    }

    @Deprecated("Use goBack node instead.")
    inner class Repeat(id: Int): Node(id)

    inner class Function(
            id: Int,
            val lambda: (Context.(Function) -> Transition)
    ): Node(id) {
        constructor(lambda: (Context.(Function) -> Transition)) : this(nextId--, lambda)
        fun exec(context: Context): Transition =
                run(context, this) { lambda(context, this) } as Transition
    }

    open inner class Command(
            id: Int,
            open val command: String,
            open val code: String
    ): TransitNode(id) {
        constructor(command: String, code: String) : this(nextId--, command, code)
    }

    inner class SubDialogue(
            id: Int,
            val dialogueId: String,
            val lambda: Context.(SubDialogue) -> PropertyMap): TransitNode(id) {

        fun getConstructorArgs(context: Context): PropertyMap =
                run(context, this) { lambda(context, this) } as PropertyMap

        fun create(vararg args: Pair<String, Any>): PropertyMap = args.toMap()
    }

    inner class StartDialogue(id: Int) : TransitNode(id)

    open inner class GoBack(id: Int, val repeat: Boolean = false) : Node(id)

    inner class StopDialogue(id: Int) : Node(id)

    inner class StopSession(id: Int) : Node(id)

    inner class Sleep(id: Int, val timeout: Int = 60) : TransitNode(id)

    val dialogueNameWithoutVersion get() = with (dialogueName) {
        if (count { it == '/' } > 1)
            substringBeforeLast("/")
        else
            this
    }

    @Deprecated("Use dialogueName instead", ReplaceWith("dialogueName"))
    open val name get() = dialogueName

    @Deprecated("Use dialogueNameWithoutVersion instead", ReplaceWith("dialogueNameWithoutVersion"))
    val nameWithoutVersion get() = dialogueNameWithoutVersion

    open val version = 0

    val intents: List<Intent> get() = nodes.filterIsInstance<Intent>()

    val globalIntents: List<GlobalIntent> get() = nodes.filterIsInstance<GlobalIntent>()

    val actions: List<Action> get() = nodes.filterIsInstance<Action>()

    val globalActions: List<GlobalAction> get() = nodes.filterIsInstance<GlobalAction>()

    val userInputs: List<UserInput> get() = nodes.filterIsInstance<UserInput>()

    val responses: List<Response> get() = nodes.filterIsInstance<Response>()

    val functions: List<Function> get() = nodes.filterIsInstance<Function>()

    val commands: List<Command> get() = nodes.filterIsInstance<Command>()

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
            sb.append(it.key).append(" = ").appendLine(it.value)
        }
        return sb.toString()
    }

    class Run(val node: Node, val context: Context)

    class DialogueScriptException(node: Node, cause: Throwable) : Throwable("DialogueScript failed at ${node.dialogue.dialogueName}:${node.dialogue.version}#${node.id}", cause)

    companion object {

        const val GENERATED_USER_INPUT_ID = 10000

        val defaultNamespace: String = "_default"
        @Deprecated("Use `defaultNamespace` instead of `clientNamespace`")
        val clientNamespace = defaultNamespace

        private val _run = ThreadLocal<Run>()

        val isRunning get() = (_run.get() != null)

        val run get() = _run.get() ?: error("dialogue code not running")

        fun ifRunning(block: Run.() -> Unit) {
            if (isRunning)
                block(run)
        }

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

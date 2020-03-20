package com.promethist.core.nlp

import com.promethist.core.runtime.Loader
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

open class Dialogue(open val loader: Loader, open val name: String) {

    val nodes: MutableSet<Node> = mutableSetOf()
    var nextId: Int = 0
    var start = StartDialogue(nextId--)
    var stop = StopDialogue(Int.MIN_VALUE)

    abstract inner class Node(open val id: Int) {

        init { nodes.add(this) }

        override fun hashCode(): Int = id

        override fun toString(): String = "${javaClass.simpleName}(id=$id)"
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
            vararg intent: Intent
    ): Node(id) {
        val intents = intent

        constructor(id: Int, vararg intent: Intent) : this(id, false, *intent)
    }

    open inner class Intent(
            override val id: Int,
            val name: String,
            vararg utterance: String
    ): TransitNode(id) {
        val utterances = utterance
    }

    inner class GlobalIntent(
             override val id: Int,
             vararg utterance: String
    ): Intent(id, name, *utterance)

    open inner class Response(
            override val id: Int,
            val image: String? = null,
            val audio: String? = null,
            val video: String? = null,
            vararg text: (Context.(Response) -> String)
    ): TransitNode(id) {
        val texts = text

        constructor(id: Int, vararg text: (Context.(Response) -> String)) : this(id, null, null, null, *text)

        fun getText(context: Context, index: Int = -1): String = texts[if (index < 0) Random.nextInt(texts.size) else index](context, this)
    }

    inner class Function(
            override val id: Int,
            val lambda: (Context.(Function) -> Transition)
    ): Node(id) {
        fun exec(context: Context): Transition = lambda(context, this)
    }

    inner class SubDialogue(
            override val id: Int,
            val name: String,
            val lambda: (Context.(SubDialogue) -> Dialogue)): TransitNode(id) {

        fun createDialogue(context: Context): Dialogue = lambda(context, this)

        fun create(vararg arg: Any) = loader.newObject<Dialogue>("$name/model", *arg)
    }

    inner class StartDialogue(override val id: Int) : TransitNode(id)

    inner class StopDialogue(override val id: Int) : Node(id)

    inner class StopSession(override val id: Int) : Node(id)

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
}


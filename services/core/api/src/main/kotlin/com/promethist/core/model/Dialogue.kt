package com.promethist.core.model

import org.litote.kmongo.Id
import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

open class Dialogue(
        val created: Date = Date(),
        val author: User? = null,
        val state: State = State.Draft,
        val nodes: MutableSet<Node> = mutableSetOf()
    )
{
    private var nextNodeId: Int = 1

    interface Function {
        fun exec(context: Context): Dialogue.Node
    }

    enum class State { Draft, Public }

    abstract inner class Node(open val id: Int) {
        lateinit var nextNode: Node
        val attributes: MutableMap<String, Any> = mutableMapOf()

        init {
            nodes.add(this)
        }

        override fun hashCode(): Int = id

        override fun toString(): String = "${javaClass.simpleName}(id=$id)"
    }

    inner class Intent(
            override val id: Int = nextNodeId++,
            val utterances: List<String>
    ): Node(id)

    open inner class Response(
            override val id: Int = nextNodeId++,
            open var texts: List<String>,
            open var isSsml: Boolean = false
    ): Node(id)

    inner class ResourceResponse(
            override val id: Int = nextNodeId++,
            override var texts: List<String>,
            override var isSsml: Boolean = false,
            val resource_id: Id<*>
    ): Response(id, texts, isSsml)

    abstract inner class ObjectFunction(
            override val id: Int = nextNodeId++
    ): Node(id), Function {
        abstract override fun exec(context: Context): Node
    }

    inner class LambdaFunction(
            override val id: Int = nextNodeId++,
            val lambda: (Function.(Context) -> Node)
    ): Node(id), Function {
        override fun exec(context: Context): Node = lambda(context)
    }

    inner class SubDialogue(override val id: Int, val name: String): Node(id)

    inner class StopDialogue(override val id: Int = nextNodeId++) : Node(id)

    inner class StopSession(override val id: Int = nextNodeId++) : Node(id)

    val intents: List<Intent> get() = nodes.filter { it is Intent }.map { it as Intent }

    val responses: List<Response> get() = nodes.filter { it is Response }.map { it as Response }

    val functions: List<Function> get() = nodes.filter { it is Function }.map { it as Function }

    val subDialogues: List<SubDialogue> get() = nodes.filter { it is SubDialogue }.map { it as SubDialogue }

    fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this@Revision")

    val properties: List<KMutableProperty<*>>
        get() = javaClass.kotlin.members.filter {
            it is KMutableProperty && !it.returnType.isSubtypeOf(Dialogue.Node::class.createType())
        }.map { it as KMutableProperty<*>}

    fun intent(id: Int = nextNodeId++, utterances: List<String>): Intent = Intent(id, utterances)

    fun response(id: Int = nextNodeId++, texts: List<String>, isSsml: Boolean = false): Response = Response(id, texts, isSsml)

    fun function(id: Int = nextNodeId++, function: (Function.(Context) -> Node)): Node = LambdaFunction(id, function)

    override fun toString(): String = "${javaClass.simpleName}(state=$state, nodes=$nodes)"
}

fun dialogue(init: (Dialogue.() -> Dialogue.Node)): Dialogue {
    val dialogue = Dialogue()
    init.invoke(dialogue)
    return dialogue
}

package com.promethist.core.model

import com.promethist.core.ResourceLoader
import org.litote.kmongo.Id
import org.slf4j.Logger

open class Dialogue(open val resourceLoader: ResourceLoader, open val name: String) {

    val nodes: MutableSet<Node> = mutableSetOf()
    var nextId: Int = 0
    var start = StartDialogue(nextId++)
    var stop = StopDialogue(Int.MAX_VALUE)

    abstract inner class Node(open val id: Int) {

        init {
            nodes.add(this)
        }

        override fun hashCode(): Int = id

        override fun toString(): String = "${javaClass.simpleName}(id=$id)"
    }

    abstract inner class TransitNode(override val id: Int): Node(id) {
        lateinit var next: Node
    }

    data class Transition(val node: Node)

    inner class Fork(
            override val id: Int,
            vararg intent: Intent
    ): Node(id) {
        val intents = intent
    }

    open inner class Intent(
            override val id: Int,
            vararg utterance: String
    ): TransitNode(id) {
        val utterances = utterance
    }

    inner class GlobalIntent(
             override val id: Int,
             vararg utterance: String
    ): Intent(id, *utterance)

    open inner class Response(
            override val id: Int,
            vararg text: ((Context) -> String)
    ): TransitNode(id) {
        val texts = text
    }

    inner class ImageResponse(
            override val id: Int,
            val image: String,
            vararg text: ((Context) -> String)
    ): Response(id, *text)

    inner class Function(
            override val id: Int,
            val lambda: (Function.(Context, Logger) -> Transition)
    ): Node(id) {
        fun exec(context: Context, logger: Logger): Transition = lambda(context, logger)
    }

    inner class SubDialogue(
            override val id: Int,
            val name: String,
            val lambda: (SubDialogue.() -> Dialogue)): TransitNode(id) {

        val dialogue: Dialogue get() = lambda(this)

        fun create(vararg arg: Any) = resourceLoader.newObject<Dialogue>("$name/model", *arg)
    }

    inner class StartDialogue(override val id: Int) : TransitNode(id)

    inner class StopDialogue(override val id: Int) : Node(id)

    inner class StopSession(override val id: Int) : Node(id)

    val intents: List<Intent> get() = nodes.filterIsInstance<Intent>()

    val globalIntents: List<GlobalIntent> get() = nodes.filterIsInstance<GlobalIntent>()

    val responses: List<Response> get() = nodes.filterIsInstance<Response>()

    val functions: List<Function> get() = nodes.filterIsInstance<Function>()

    val subDialogues: List<SubDialogue> get() = nodes.filterIsInstance<SubDialogue>()

    fun node(id: Int): Node = nodes.find { it.id == id }?:error("Node $id not found in $this")
    /*
    val properties: List<KMutableProperty<*>>
        get() = javaClass.kotlin.members.filter {
            it is KMutableProperty && !it.returnType.isSubtypeOf(Node::class.createType())
        }.map { it as KMutableProperty<*>}
    */

    fun validate() {
        for (node in nodes) {
            try {
                node is TransitNode && node.next == null
            } catch (e: UninitializedPropertyAccessException) {
                error("${this::class.qualifiedName}.${node} missing next node reference")
            }
        }
    }
}


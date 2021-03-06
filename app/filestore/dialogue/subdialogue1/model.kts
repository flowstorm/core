package `subdialogue1`

import ai.flowstorm.core.dialogue.Dialogue
import ai.flowstorm.core.type.Location

data class Model1(
        // dialogue properties
        val i: Int = 1

) : Dialogue() {

    override val dialogueName: String = "product/some-subdialogue"
    override val clientLocation: Location? = null

    val response0 = Response(nextId--, { "Welcome to sub dialogue" })
    val intent1 = Intent(nextId--, "intent1", "yes", "okay")
    val intent2 = Intent(nextId--, "intent2", "no", "nope")
    val input1 = UserInput(nextId--, arrayOf(intent1, intent2)) {
        processPipeline()
        null
    }
    val response1 = Response(nextId--, { "Bye from sub dialogue" })

    init {
        start.next = response0
        response0.next = input1
        intent1.next = response1
        intent2.next = response1
        response1.next = stop
    }
}

// export
Model1::class
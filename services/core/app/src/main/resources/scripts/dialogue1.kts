//-- start of build output - instance based version (nodes are just dialogue instance variables)
import com.promethist.core.model.*

val d = dialogue {

    val intent1 = intent(id = -1/* custom id from editor*/, utterances = listOf("hi", "hello"))
    val response1 = response(texts = listOf("welcome", "nice to meet you"))
    val function1 = function { context ->
        // start of script
        if (true)
            intent1
        else
            StopDialogue()
        // end of script
    }

    // node references
    intent1.next = response1
    response1.next = function1

    intent1 // start node
}
//-- end of build output

println(d)
println(d.intents)
println((d.node(2) as Dialogue.Function).exec(Context(message = "hello")))
import com.promethist.core.model.*

dialogue {
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
    intent1.nextNode = response1
    response1.nextNode = function1

    intent1 // start node
}
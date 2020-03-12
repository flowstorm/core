//-- start of build output - class based version (nodes are dialogue properties)
import com.promethist.core.model.*

class Dialogue2 : Dialogue() {// we can inherit from existing dialogues
    // dialogue properties (always var)
    var some_string = "blah"
    var math_max = 10
    var do_math = true

    // dialogue functions
    fun someUsefulFunction(intent: Intent): Intent = intent

    // dialogue nodes (always val named by editor elements)
    val intent1 = Intent(id = -1/* custom id from editor*/, utterances = listOf("hi", "hello"))
    val response1 = Response(texts = listOf("welcome", "nice to meet you"))
    val function1 = object : ObjectFunction() { // descendants of ObjectFunction can be librarized
        override fun exec(context: Context): Node {
            // start of editable script
            return if (context.message.startsWithVowel())
                someUsefulFunction(intent1)
            else
                function2
            // end of editable script
        }
    }
    val function2 = LambdaFunction { context -> // simple lambda function (always one purpose, embedded)
        // start of editable script
        if (do_math && !context.message.isBlank())
            intent1
        else
            StopDialogue()
        // end of editable script
    }

    // dialogue node references
    init {
        start.next = intent1
        intent1.next = response1
        response1.next = function1
    }
}

// global class extensions
fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

//-- end of build output
val d = Dialogue2()
println(d)
println(d.properties)



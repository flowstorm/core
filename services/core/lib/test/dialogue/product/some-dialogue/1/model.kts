package `product`.`some-dialogue`

import com.promethist.core.runtime.Loader
import com.promethist.core.model.*

data class Model1(
        override val loader: Loader,
        override val name: String,

        // dialogue properties
        val some_string: String = "blah",
        val math_max: Int = 10,
        val do_math: Boolean = true

) : Dialogue(loader, name) {

    // dialogue functions and local values (declared in editor dialogue init section)
    val data = loader.loadObject("$name/data")

    fun someUsefulFunction(intent: Intent): Intent = intent

    // dialogue nodes (always val named by editor elements)
    val globalIntent1 = GlobalIntent(nextId--, "volume up")
    val globalIntent2 = GlobalIntent(nextId--, "volume down")
    val response0 = Response(nextId--, { "Hi, this is Jarmila" })
    val intent1 = Intent(nextId--,"yes", "okay")
    val intent2 = Intent(nextId--, "no", "nope")
    val input1 = UserInput(nextId--, intent1, intent2)

    val response1 = Response(nextId--,
            { """welcome back ${session.user.name}""" },
            { """hello back${session.user.name}""" }
    )
    val response2 = Response(nextId--,
            { """be nicer ${session.user.name}""" },
            { """just be nicer ${session.user.name}""" }
    )

    val function1 = Function(nextId--) { self -> // simple lambda function (always one purpose, embedded)

        val trans1 = Transition(input1)
        val trans2 = Transition(stop)

        //-- start of dialogue script
        println("context = $context")
        println("data = $data")
        self.let {
            println("function.id = ${it.id}, dialogue.name = $name")
        }
        if (do_math)
            trans1
        else
            trans2
        //-- end of dialogue script
    }

    val subDialogue1 = SubDialogue(nextId--,  "product/some-subdialogue/1") { self ->
        //-- start of dialogue script (if text in editor is empty, then just create() will be inserted
        self.create(math_max)
        //-- end of dialogue script
    }

    init {
        // transit node references
        start.next = response0
        response0.next = input1
        globalIntent1.next = response1
        globalIntent2.next = response2
        intent1.next = response1
        intent2.next = response2
        response1.next = function1
        response2.next = input1
        subDialogue1.next = input1
    }
}

// global class extensions
fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

Model1::class
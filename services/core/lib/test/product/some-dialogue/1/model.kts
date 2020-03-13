package `product`.`some-dialogue`

import com.promethist.core.ResourceLoader
import com.promethist.core.model.*

data class Model1(
        override val resourceLoader: ResourceLoader,
        override val name: String,

        // dialogue properties
        val some_string: String = "blah",
        val math_max: Int = 10,
        val do_math: Boolean = true

) : Dialogue(resourceLoader, name) {

    // dialogue functions and local values (declared in editor dialogue init section)
    val data = resourceLoader.loadObject("$name/data")

    fun someUsefulFunction(intent: Intent): Intent = intent

    // dialogue nodes (always val named by editor elements)
    val globalIntent1 = GlobalIntent(nextId++, "volume up")
    val globalIntent2 = GlobalIntent(nextId++, "volume down")
    val intent1 = Intent(nextId++,"yes", "okay")
    val intent2 = Intent(nextId++, "no", "nope")
    val fork1 = Fork(nextId++, intent1, intent2)

    val response1 = Response(nextId++,
            { context -> """welcome ${context.session.user.name}""" },
            { context: Context -> """nice to meet you ${context.session.user.name}""" }
    )
    val response2 = Response(nextId++,
            { context -> """be nicer ${context.session.user.name}""" },
            { context: Context -> """nice to meet you ${context.session.user.name}""" }
    )

    val function1 = Function(nextId++) { context, logger -> // simple lambda function (always one purpose, embedded)

        val trans1 = Transition(fork1)
        val trans2 = Transition(stop)

        //-- start of dialogue script
        println(data)
        if (do_math && !context.message.isBlank())
            trans1
        else
            trans2
        //-- end of dialogue script
    }

    val subDialogue1 = SubDialogue(nextId++,  "product/some-subdialogue/1") {
        //-- start of dialogue script (if text in editor is empty, then just create() will be inserted
        create(math_max)
        //-- end of dialogue script
    }

    init {
        //fork1.nextNodes.forEach { println(it) }
        // dialogue node references (note for build implementation - transit nodes without transition defined should not be
        start.next = fork1
        globalIntent1.next = response1
        globalIntent2.next = response2
        intent1.next = response1
        intent2.next = response2
        response1.next = function1
        response2.next = fork1
        subDialogue1.next = stop
    }
}

// global class extensions
fun String.startsWithVowel() = Regex("[aioy].*").matches(this)

Model1::class
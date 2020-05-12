package `product`.`some-dialogue`

import com.promethist.core.dialogue.*
import com.promethist.core.type.*

data class Model1(
        // dialogue properties
        val some_string: String = "blah",
        val math_max: Int = 10,
        val do_math: Boolean = true

) : BasicDialogue() {
    override val name: String = "product/some-dialogue/1/model"

    //val movies by loader<List<Movie>>("file:///Users/tomas.zajicek/Projects/promethist-main/local/movies.json")
    val movies by loader<List<PropertyMap>>("https://raw.githubusercontent.com/vega/vega/master/docs/data/movies.json")

    // dialogue functions and local values (declared in editor dialogue init section)
    val data by loader<Map<String, Number>>("$name/data")

    var testx0 by sessionAttribute<Int>() // default value will be 0
    var testx1 by sessionAttribute { 1 } // specific default value (not 0)
    var testx2 by sessionAttribute { "Samuel" } // example of specific string default value
    var testx3 by sessionAttribute<Int>("appname") // app counter (used by multiple dialogues)

    fun someUsefulFunction(intent: Intent): Intent = intent

    // dialogue nodes (always val named by editor elements)
    val globalIntent1 = GlobalIntent(nextId--, "globalIntent1", "volume up")
    val globalIntent2 = GlobalIntent(nextId--, "globalIntent2", "volume down")
    val response0 = Response(nextId--, { """Hi, this is Jarmila defined by ${name}""" })
    val intent1 = Intent(nextId--, "intent1","yes", "okay")
    val intent2 = Intent(nextId--, "intent2", "no", "nope")
    val input1 = UserInput(nextId--, arrayOf(intent1, intent2)) {
        processPipeline()
        null
    }

    val response1 = Response(nextId--,
            { """welcome back ${session.user.name}, you said: ${input.transcript.text}""" },
            { """hello back${session.user.name}""" }
    )
    val response2 = Response(nextId--,
            { """be nicer ${session.user.name}""" },
            { """just be nicer ${session.user.name}""" }
    )

    val function1 = Function(nextId--) { // simple lambda function (always one purpose, embedded)

        val trans1 = Transition(input1)
        val trans2 = Transition(stop)

        //-- start of dialogue script
        println(movies of "movie")
        println(1 of "time")
        println(enumerate(data))
        println(describe(data))
        println(enumerateWithArticle(listOf("a1", "a2", "a3"), "value"))
        println("turn = $turn")
        println("data = $data")
        //movies.forEach { println(it["Title"]) }
        println("session.attributes = ${session.attributes}")
        testx0++
        testx1++
        testx2 = "Eleanor"
        testx3++
        println("session.attributes = ${session.attributes}")
        it.let {
            println("function.id = ${it.id}, dialogue.name = $name")
        }
        if (do_math)
            trans1
        else
            trans2
        //-- end of dialogue script
    }

    val subDialogue1 = SubDialogue(nextId--,  "product/some-subdialogue/1") {
        //-- start of dialogue script (if text in editor is empty, then just it.create() will be inserted
        it.create("math_max" to math_max)
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

Model1::class
package com.promethist.core.runtime

import com.promethist.core.Input
import com.promethist.core.dialogue.Dialogue.Companion.similarityTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DialogueScriptTest {

    data class Movie(val name: String, val director: String, val year: Int)

    val movies = listOf(
            Movie("Blade Runner", "Ridley Scott", 1982),
            Movie("Blade Runner 2049", "Denis Villeneuve", 2017),
            Movie("The Indian Runner", "Sean Penn", 1991),
            Movie("Runner", "Bill Gallagher", 2019)
    )

    @Test
    fun `test similarity`() {
        val input = Input()
        input.tokens.add(Input.Word("blade"))
        //input.tokens.add(Input.Word("runner"))

        println(movies.filter { it.name similarityTo input >= 0.5 }.maxBy { it.name similarityTo input }.let { movie ->
            if (movie != null) {
                val favoriteMovie = movie.name
                "So you like $favoriteMovie. Did you know that the movie was shot by director ${movie.director}?"
            } else {
                "Unfortunately I don't know such movie."
            }
        })

        println(movies.maxBy { it.name similarityTo input }) // movie with highest similarity of name
        println(movies.sortedByDescending { it.name similarityTo input }.take(2)) // take first two movies with highest similarity

        println(movies.map { it.name similarityTo input })
        println(movies.find { it.name similarityTo input >= 0.5 })
    }
}
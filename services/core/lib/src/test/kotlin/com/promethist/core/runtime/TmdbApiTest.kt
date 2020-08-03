package com.promethist.core.runtime

import com.promethist.core.dialogue.DialogueTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TmdbApiTest: DialogueTest() {

    @Test
    fun testSearch() {
        println(dialogue.tmdb.search("Matrix"))
        println(dialogue.tmdb.credits("Matrix"))
        println(dialogue.tmdb.personCredits("Nicolas Cage"))
        println(dialogue.tmdb.popularMovies(LocalDate.of(1900, 1, 1)))
        println(dialogue.tmdb.movieTitle(603))
    }

    @Test
    fun testList() {
        println(dialogue.tmdb.credits("Matrix").list("cast")[0])

    }
}
package ai.flowstorm.core.runtime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ai.flowstorm.core.dialogue.DialogueTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TmdbApiTest: DialogueTest() {

    @Test
    fun testSearch() {
//        println(dialogue.tmdb.search("Matrix"))
//        println(dialogue.tmdb.credits("Matrix"))
//        println(dialogue.tmdb.personCredits("Nicolas Cage"))
//        println(dialogue.tmdb.popularMovies(LocalDate.of(1900, 1, 1)))
//        println(dialogue.tmdb.movieTitle(603))
    }

    @Test
    fun testList() {
//        println(dialogue.tmdb.credits("Matrix").list("cast")[0])

    }
}
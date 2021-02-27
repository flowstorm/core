package ai.flowstorm.core.dialogue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SimilarityTest {

    data class Movie(val name: String, val director: String, val year: Int)

    val movies = listOf(
            Movie("Blade Runner", "Ridley Scott", 1982),
            Movie("Blade Runner 2049", "Denis Villeneuve", 2017),
            Movie("The Indian Runner", "Sean Penn", 1991),
            Movie("Runner", "Bill Gallagher", 2019),
            Movie("Some Movie", "Some Director", 2010)
    )

    val fruits = listOf("yellow banana", "yellow pear", "red apple")

    @Test
    fun `similarTo`() {

        var input = "runner"

        println(movies.map { "${it.name} = ${(it.name similarityTo input)}" })
        println(movies.similarTo(input, { name }, 0.5))
        println(movies.similarTo(input, { name }, 5, 0.5))


        input = "yellow"
        println(fruits.map { "$it = ${it similarityTo input}"})
        println(fruits.similarTo(input, 0.5))
        println(fruits.similarTo(input, 5, 0.5))

        //assertEquals(1, metrics.size)

        println()
    }

}
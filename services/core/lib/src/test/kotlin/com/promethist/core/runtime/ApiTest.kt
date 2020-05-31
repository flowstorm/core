package com.promethist.core.runtime

import com.promethist.core.type.Dynamic
import com.promethist.core.type.PropertyMap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApiTest {

    @Test
    fun `test api words`() {

        println(Api.get<List<String>>(Api.target("https://repository.promethist.ai/data/animals.json")))

        println(Api.words("good", "antonyms"))

        val response = Api.words<Dynamic>("hatchback")
        response<List<PropertyMap>>("results") {
            value.forEach {
                println("partOfSpeech = " + it["partOfSpeech"])
                if (it.containsKey("synonyms")) {
                    println("synonyms:")
                    (it["synonyms"] as ArrayList<String>).forEach {
                        println("- $it")
                    }
                }
            }
        }

    }
}
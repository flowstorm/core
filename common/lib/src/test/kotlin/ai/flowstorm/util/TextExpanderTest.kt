package ai.flowstorm.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextExpanderTest {

    @ParameterizedTest
    @MethodSource
    fun `string is expanded correctly`(sample: Pair<String, List<String>>) {
        assertEquals(sample.second.sorted(), TextExpander.expand(sample.first).sorted())
    }

    fun `string is expanded correctly`(): List<Pair<String, List<String>>> = listOf(
        "(hi|hello|) how are you" to listOf(
            "hi how are you",
            "hello how are you",
            "how are you"
        ),
        "(hi|hello|) how are you (|tell me something)" to listOf(
            "hi how are you",
            "hello how are you",
            "how are you",
            "hi how are you tell me something",
            "hello how are you tell me something",
            "how are you tell me something"
        ),
        "(hi|hello|how are you (|doing)|) (what is|what's) your name" to listOf(
            "hi what is your name",
            "hello what is your name",
            "how are you what is your name",
            "how are you doing what is your name",
            "what is your name",
            "hi what's your name",
            "hello what's your name",
            "how are you what's your name",
            "how are you doing what's your name",
            "what's your name"
        ),
        "(hi|hello ((my|) boy|)|)" to listOf(
            "hi",
            "hello",
            "hello boy",
            "hello my boy"
        ),
        "(something (a|b)|something else (c|d)) end" to listOf(
            "something a end",
            "something b end",
            "something else c end",
            "something else d end"
        ),
        "text before (a|b) text (c|d) text (e|f|g) text after" to listOf(
            "text before a text c text e text after",
            "text before a text c text f text after",
            "text before a text c text g text after",
            "text before a text d text e text after",
            "text before a text d text f text after",
            "text before a text d text g text after",
            "text before b text c text e text after",
            "text before b text c text f text after",
            "text before b text c text g text after",
            "text before b text d text e text after",
            "text before b text d text f text after",
            "text before b text d text g text after"
        )
    )

    @Test
    fun `throws exception when number of opening and closing parenthesis does not match`() {
        assertThrows(IllegalArgumentException::class.java) {
            TextExpander.expand("(hi|hello| how are you")
        }

        assertThrows(IllegalArgumentException::class.java) {
            TextExpander.expand("(hi|hello| how are you) )")
        }
    }
}
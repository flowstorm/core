package ai.flowstorm.core.dialogue

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParaphrasingTest: DialogueTest() {
    @Test
    fun `test paraphrasing`() {
        with(dialogue) {
            assert(paraphrasing.isParaphrasable("I like you"))
            assert(paraphrasing.paraphrase("I like you").endsWith("you like me."))
            assert(!paraphrasing.isParaphrasable("green is a nice color"))
            assert(paraphrasing.paraphrase("green is a nice color").endsWith("green is a nice color?"))
            assert(paraphrasing.isParaphrasable("what do you mean"))
            assert(paraphrasing.paraphrase("what do you mean").endsWith("what I mean?"))
        }
    }
}
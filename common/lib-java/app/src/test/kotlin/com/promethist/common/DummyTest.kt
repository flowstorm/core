package com.promethist.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DummyTest {
    //Dummy test to keep test directory in git and avoid warnings in build until we introduce actual tests.

    @Test
    fun `dummy test case`() {
        assert(true)
    }
}
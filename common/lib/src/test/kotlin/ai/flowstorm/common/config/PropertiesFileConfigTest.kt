package ai.flowstorm.common.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertiesFileConfigTest {

    private val config = PropertiesFileConfig()

    @Test
    fun `getOrNull returns null when key is not in config`() {
        val a = config.getOrNull("non.existing")
        assertNull(a)
    }

    @Test
    fun `getOrNull returns value when key is in config`() {
        val a = config.getOrNull("property1")
        assertEquals(a, "property1")
    }
}
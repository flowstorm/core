package ai.flowstorm.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppConfigTest {

    private val appConfig = AppConfig()

    @Test
    fun `getOrNull returns null when key not in config`() {
        val a = appConfig.getOrNull("non.existing")
        assertNull(a)
    }

    @Test
    fun `getOrNull null when key not in config`() {
        val a = appConfig.getOrNull("property1")
        assertEquals(a, "property1")
    }
}
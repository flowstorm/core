package ai.flowstorm.common.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigTest {

    private val config = object :Config {
        override fun getOrNull(key: String): String? = when(key) {
            "non.existing" -> null
            "property" -> "propertyValue"
            else -> error("")
        }

        override fun set(key: String, value: String) = error("Not tested")

    }

    @Test
    fun `get returns default when key is not in config`() {
        val value = config.get("non.existing", "default")
        assertEquals("default", value)
    }

    @Test
    fun `get returns value when key is in config`() {
        val value = config.get("property", "default")
        assertEquals("propertyValue", value)
    }

    @Test
    fun `get throws when key is not in config`() {
        assertThrows<NullPointerException> {
            config["non.existing"]
        }
    }
}
package org.promethist.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.promethist.security.Digest
import java.util.*

data class TtsConfig(
        val provider: String,
        val locale: Locale,
        val gender: Gender,
        val name: String,
        val engine: String? = null
) {
    enum class Gender { Male, Female }

    @get:JsonIgnore
    val language: String get() = locale.language

    @get:JsonIgnore
    val code = Digest.md5((provider + locale.toString() + gender.name + name + engine).toByteArray())
}

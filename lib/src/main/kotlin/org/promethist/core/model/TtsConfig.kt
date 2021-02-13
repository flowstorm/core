package org.promethist.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.promethist.security.Digest
import java.util.*

data class TtsConfig(
        val provider: String,
        val locale: Locale,
        val gender: Gender,
        val name: String,
        val engine: String? = null,
        val amazonAlexaVoice: String? = null,
        val googleAssistantVoice: String? = null
) {
    constructor(provider: String, locale: Locale, gender: Gender, name: String, engine: String?) :
            this(provider, locale, gender, name, engine, null, null)

    enum class Gender { Male, Female }

    @get:JsonIgnore
    val language: String get() = locale.language

    @get:JsonIgnore
    val code = Digest.md5((provider + locale.toString() + gender.name + name + engine).toByteArray())
}

package ai.flowstorm.core.model

import com.fasterxml.jackson.annotation.JsonIgnore
import ai.flowstorm.security.Digest
import java.util.*

data class TtsConfig(
    val provider: String,
    val locale: Locale,
    val gender: Gender,
    val name: String,
    val engine: String? = null,
    val amazonAlexa: String? = null,
    val googleAssistant: String? = null
) {
    constructor(provider: String, locale: Locale, gender: Gender, name: String, engine: String?) :
            this(provider, locale, gender, name, engine, null, null)

    enum class Gender { Male, Female }

    @get:JsonIgnore
    val language: String get() = locale.language

    @get:JsonIgnore
    val code = Digest.md5((provider + locale.toString() + gender.name + name + engine).toByteArray())
}

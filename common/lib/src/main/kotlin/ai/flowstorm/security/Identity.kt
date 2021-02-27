package ai.flowstorm.security

open class Identity(
    open val username: String,
    open val name: String? = null,
    open val surname: String? = null,
    open val nickname: String? = null,
    open val phoneNumber: String? = null
)
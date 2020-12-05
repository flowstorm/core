package org.promethist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT

class JwtToken internal constructor(username: String, val decodedJWT: DecodedJWT) : Identity(username) {

    companion object {
        fun create(token: String) = JWT.decode(token).let { decodedJWT ->
            JwtToken(decodedJWT.getClaim("https://promethist/user.email").let {
                (if (it.isNull) decodedJWT.getClaim("email") else it).asString()
            }, decodedJWT)
        }
    }

    val payload = Payload(decodedJWT)
    override val name: String get() = payload.given_name
    override val surname: String get() = payload.family_name
    override val nickname: String get() = payload.nickname

    data class Payload(private val decodedJWT: DecodedJWT) {
        val username = decodedJWT.getClaim("email").asString()
        val given_name = decodedJWT.getClaim("given_name").asString()
        val family_name = decodedJWT.getClaim("family_name").asString()
        val nickname = decodedJWT.getClaim("nickname").asString()
        val name = decodedJWT.getClaim("name").asString()
        val picture = decodedJWT.getClaim("picture").asString()
        val iat = decodedJWT.getClaim("iat").asInt()
        val exp = decodedJWT.getClaim("exp").asInt()
    }
}
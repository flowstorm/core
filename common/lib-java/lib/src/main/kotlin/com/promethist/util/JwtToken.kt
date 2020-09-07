package com.promethist.util

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT

class JwtToken(val decodedJWT: DecodedJWT) {
    companion object {
        fun createFromHeaderString(header: String): JwtToken {
            return JwtToken(JWT.decode(header.substring(7)))
        }
        fun createFromString(token: String): JwtToken {
            return JwtToken(JWT.decode(token))
        }
    }
    val username: String
        get() {
            //backward compatibility we switched from id_token to access_token
            var claim = decodedJWT.getClaim("https://promethist/user.email")
            if (claim.isNull) claim = decodedJWT.getClaim("email")

            return claim.asString()
        }
    val payload: Payload
        get() = Payload(decodedJWT)
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
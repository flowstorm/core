package ai.flowstorm.common.security

import com.auth0.jwt.exceptions.JWTVerificationException
import ai.flowstorm.security.JwtToken
import ai.flowstorm.security.jwt.JwtVerifier
import javax.inject.Inject
import javax.ws.rs.NotAuthorizedException

class JwtAccessTokenAdapter : AbstractTokenAdapter() {

    @Inject
    lateinit var jwtVerifier: JwtVerifier

    override fun authorize() {
        val jwt = JwtToken.create(token)

        try {
            jwtVerifier.verify(jwt.decodedJWT)
        } catch (e: JWTVerificationException) {
            throw NotAuthorizedException(e.message, e, SCHEME)
        }
    }
}
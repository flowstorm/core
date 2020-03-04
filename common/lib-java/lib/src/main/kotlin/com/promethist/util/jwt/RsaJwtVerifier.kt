package com.promethist.util.jwt

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class RsaJwtVerifier(private val issuer:String) : JwtVerifier {

    private val jwkProvider = JwkProviderBuilder(issuer)
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    private val verifiers = mutableMapOf<String, JWTVerifier>()

    override fun verify(jwt: DecodedJWT): DecodedJWT {
        val kid = jwt.getHeaderClaim("kid").asString();

        return getVerifier(kid).verify(jwt)
    }

    private fun getVerifier(kid: String): JWTVerifier {
        if (!verifiers.contains(kid)) {
            val jwk = jwkProvider.get(kid)
            val algorithm = Algorithm.RSA256(jwk.publicKey as RSAPublicKey, null)
            val verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
            verifiers[kid] = verifier
        }

        return verifiers.getValue(kid)
    }
}
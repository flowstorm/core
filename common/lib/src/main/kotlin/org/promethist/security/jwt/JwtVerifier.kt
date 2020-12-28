package org.promethist.security.jwt

import com.auth0.jwt.interfaces.DecodedJWT

interface JwtVerifier {
    fun verify(jwt: DecodedJWT): DecodedJWT
}
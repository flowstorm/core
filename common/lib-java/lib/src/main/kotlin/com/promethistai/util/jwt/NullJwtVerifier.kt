package com.promethistai.util.jwt

import com.auth0.jwt.interfaces.DecodedJWT

class NullJwtVerifier : JwtVerifier {
    override fun verify(jwt: DecodedJWT): DecodedJWT {
        // do not verify the token
        return jwt
    }
}
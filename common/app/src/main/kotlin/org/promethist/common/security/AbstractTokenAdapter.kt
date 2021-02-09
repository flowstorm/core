package org.promethist.common.security

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders

abstract class AbstractTokenAdapter : AuthorizationAdapter {

    @Context
    lateinit var context: ContainerRequestContext

    val token: String
        get() {
            val authorizationHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION)

            if (authorizationHeader == null || !authorizationHeader.startsWith("$SCHEME "))
                throw AuthorizationAdapter.AuthorizationFailed("Missing authentication token.")

            return authorizationHeader.substringAfter("$SCHEME ").trim()
        }

    companion object {
        const val SCHEME = "Bearer"
    }
}
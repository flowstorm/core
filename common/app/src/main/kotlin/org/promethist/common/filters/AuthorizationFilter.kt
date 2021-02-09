package org.promethist.common.filters

import org.promethist.common.security.AuthorizationAdapter
import org.promethist.common.security.AuthorizationAdapter.AuthorizationFailed
import org.promethist.common.security.Authorized
import java.lang.Exception
import javax.inject.Inject
import javax.ws.rs.NotAuthorizedException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@Authorized
class AuthorizationFilter : ContainerRequestFilter {
    @Inject
    lateinit var adapterProvider: javax.inject.Provider<AuthorizationAdapter>

    private val adapter get() = adapterProvider.get() ?: throw Exception("Missing binding for AuthorizationAdapter.")

    override fun filter(requestContext: ContainerRequestContext) {
        try {
            adapter.authorize()
        } catch (t: AuthorizationFailed) {
            throw NotAuthorizedException("Not Authorized", Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }
}
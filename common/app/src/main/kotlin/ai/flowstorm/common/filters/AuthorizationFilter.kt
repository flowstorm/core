package ai.flowstorm.common.filters

import ai.flowstorm.common.security.AuthorizationAdapter
import ai.flowstorm.common.security.AuthorizationAdapter.AuthorizationFailed
import ai.flowstorm.common.security.Authorized
import ai.flowstorm.common.security.NoAuthorizationAdapter
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

    //TODO require explicitly bind AuthorizationAdapter
    private val adapter get() = adapterProvider.get() ?: NoAuthorizationAdapter() // error("Missing binding for AuthorizationAdapter.")

    override fun filter(requestContext: ContainerRequestContext) {
        try {
            adapter.authorize()
        } catch (t: AuthorizationFailed) {
            throw NotAuthorizedException("Not Authorized", Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }
}
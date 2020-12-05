package org.promethist.common.filters

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.ext.Provider
import javax.ws.rs.core.Response

/**
 * Filter to handle CORS pre-flight request, we detect pre-flight request and abort request with response OK (200).
 * It is useless (and time consuming) return WADL response for pre-flight requests.
 */
@Provider
class CORSPreFlightFilter : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        if (requestContext.method == "OPTIONS" && requestContext.headers.containsKey("Origin")) {
            requestContext.abortWith(Response.ok().build())
        }
    }
}
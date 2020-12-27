package org.promethist.core.filters

import org.promethist.common.filters.AbstractAuthenticationFilter
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.ext.Provider

//TODO implement filter using predefined api keys
//@Provider
//@Priority(Priorities.AUTHENTICATION)
class AuthenticationFilter : AbstractAuthenticationFilter() {

    override fun filter(requestContext: ContainerRequestContext) {
    }
}
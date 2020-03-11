package com.promethist.common.query

import org.glassfish.hk2.api.Injectee
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.ServiceHandle
import javax.inject.Inject
import javax.inject.Named

class QueryInjectionResolver : InjectionResolver<QueryParams> {
    @Inject
    @Named(InjectionResolver.SYSTEM_RESOLVER_NAME)
    var systemInjectionResolver: InjectionResolver<Inject>? = null

    override fun resolve(injectee: Injectee, handle: ServiceHandle<*>?): Any? {
        return if (Query::class.java == injectee.requiredType) {
            systemInjectionResolver!!.resolve(injectee, handle)
        } else null
    }

    override fun isConstructorParameterIndicator()  = false

    override fun isMethodParameterIndicator() = false
}
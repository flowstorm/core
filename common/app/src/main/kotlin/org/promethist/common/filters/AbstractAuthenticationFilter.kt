package org.promethist.common.filters

import org.promethist.security.Authenticated
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ResourceInfo

abstract class AbstractAuthenticationFilter : ContainerRequestFilter {

    /**
     * We use @Authenticated annotation on resource interfaces, but in case we deal with sub-resource the property
     * resourceInfo.resourceClass contains actual class instead of interface.
     * This is NOT generic solution. It suppose resource class implements only one interface!
     */
    protected fun getAnnotation(resourceInfo: ResourceInfo): Authenticated? {
        val (resourceClass, resourceMethod) = if (!resourceInfo.resourceClass.isInterface &&
            resourceInfo.resourceClass.interfaces.isNotEmpty()
        ) {
            //TODO this needs to be fixed - check all interfaces or move annotations to Impl classes
            var clazz = resourceInfo.resourceClass.interfaces[0]
            if (clazz.interfaces.isNotEmpty()) clazz = clazz.interfaces[0]
            Pair(clazz, clazz.getDeclaredMethod(resourceInfo.resourceMethod.name, *resourceInfo.resourceMethod.parameterTypes))
        } else {
            Pair(resourceInfo.resourceClass, resourceInfo.resourceMethod)
        }

        return resourceMethod.getAnnotation(Authenticated::class.java)
            ?: resourceClass.getAnnotation(Authenticated::class.java)

    }
}
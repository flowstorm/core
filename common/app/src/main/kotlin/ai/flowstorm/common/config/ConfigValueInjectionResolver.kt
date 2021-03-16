package ai.flowstorm.common.config

import org.glassfish.hk2.api.Injectee
import org.glassfish.hk2.api.InjectionResolver
import org.glassfish.hk2.api.ServiceHandle
import java.lang.reflect.Constructor
import javax.inject.Inject

class ConfigValueInjectionResolver : InjectionResolver<ConfigValue> {

    @Inject
    lateinit var config: Config

    override fun resolve(injectee: Injectee, handle: ServiceHandle<*>?): Any {

        val value = config[getKey(injectee)]

        return when (injectee.requiredType) {
            String::class.java -> value
            Int::class.java -> value.toInt()
            else -> error("Unsupported type.")
        }
    }

    private fun getKey(injectee: Injectee): String = when (val elem = injectee.parent) {
        is Constructor<*> -> elem.parameterAnnotations[injectee.position][0] as ConfigValue
        else -> elem.getAnnotation(ConfigValue::class.java)
    }.key


    override fun isConstructorParameterIndicator() = true
    override fun isMethodParameterIndicator() = false
}
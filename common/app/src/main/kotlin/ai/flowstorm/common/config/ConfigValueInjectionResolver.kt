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

        val annotation = getAnnotation(injectee)

        val value = try {
            with(annotation) {
                if (default == ConfigValue.NULL) config[key] else config.get(key, default)
            }
        } catch (t: Throwable) {
            throw Exception("Can not inject config value for key `${annotation.key}` in ${injectee.injecteeClass.simpleName}", t)
        }

        return when (injectee.requiredType) {
            String::class.java -> value
            Int::class.java -> value.toInt()
            else -> error("Unsupported type.")
        }
    }

    private fun getAnnotation(injectee: Injectee) = when (val elem = injectee.parent) {
        is Constructor<*> -> elem.parameterAnnotations[injectee.position][0] as ConfigValue
        else -> elem.getAnnotation(ConfigValue::class.java)
    }

    override fun isConstructorParameterIndicator() = true
    override fun isMethodParameterIndicator() = false
}
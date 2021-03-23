package ai.flowstorm.common.config

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigValue(val key: String, val default: String = NULL) {
    companion object {

        //Workaround: Annotation parameter can't be null
        const val NULL = "___NULL___"
    }
}

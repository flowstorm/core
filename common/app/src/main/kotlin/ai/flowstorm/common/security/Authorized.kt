package ai.flowstorm.common.security

import javax.ws.rs.NameBinding

@NameBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Authorized
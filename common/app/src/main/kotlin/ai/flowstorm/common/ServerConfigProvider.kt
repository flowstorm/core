package ai.flowstorm.common

import org.glassfish.jersey.server.ResourceConfig
import javax.servlet.Servlet

interface ServerConfigProvider {

    val serverConfig: ServerConfig

    data class ServerConfig(
            val resourceConfig: ResourceConfig,
            val servlets: Map<Class<out Servlet>, String> = emptyMap()
    )
}
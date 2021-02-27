package ai.flowstorm.common

import com.fasterxml.jackson.databind.ObjectMapper
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.ServerProperties
import java.net.URI
import javax.ws.rs.ext.ContextResolver

/**
 * @author Tomas Zajicek <tomas.zajicek@promethist.ai>
 */
class NettyServer(resourceConfig: ResourceConfig) {

    private val channel =
        NettyHttpContainerProvider.createHttp2Server(
            URI.create("http://localhost:" + AppConfig.instance.get("server.port", System.getProperty("ai.flowstorm.common.server.port", "8080")) + "/"),
            resourceConfig.register(ContextResolver<ObjectMapper> {
                ObjectUtil.defaultMapper
            }).property(ServerProperties.TRACING, AppConfig.instance.get("app.tracing", "OFF")),
            null
        )

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            channel.close()
        })
    }

}

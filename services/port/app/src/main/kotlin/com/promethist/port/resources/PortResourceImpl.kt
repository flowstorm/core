package com.promethist.port.resources

import com.promethist.core.Request
import com.promethist.core.Response
import com.promethist.core.resources.CoreResource
import com.promethist.util.LoggerDelegate
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response as JerseyResponse
import javax.ws.rs.core.StreamingOutput

@Path("/")
class PortResourceImpl : PortResource {

    private val logger by LoggerDelegate()

    @Inject
    lateinit var coreResource: CoreResource

    override fun process(request: Request): Response = coreResource.process(request)

    override fun proxyFile(spec: String): JerseyResponse {
        val url = URL(spec)
        if (!url.host.endsWith(".rackcdn.com")) {
            throw WebApplicationException("$spec proxy is forbidden", Status.FORBIDDEN)
        }
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.doInput = true
        conn.connect()
        if (conn.responseCode > 399)
            throw WebApplicationException(conn.responseMessage, conn.responseCode)

        return JerseyResponse.ok(
            StreamingOutput { output ->
                try {
                    conn.inputStream.copyTo(output)
                } catch (e: Exception) {
                    throw WebApplicationException("$url proxy failed", e)
                }
            }
        ).apply {
            conn.headerFields.forEach { headerField ->
                headerField.key?.let { name ->
                    headerField.value.forEach { value ->
                        header(name, value)
                    }
                }
            }
        }.build()
    }
}
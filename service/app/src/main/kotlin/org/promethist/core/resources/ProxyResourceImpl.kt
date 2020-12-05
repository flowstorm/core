package org.promethist.core.resources

import org.promethist.util.LoggerDelegate
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.StreamingOutput

@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
class ProxyResourceImpl : ProxyResource {

    private val logger by LoggerDelegate()

    override fun proxyFile(spec: String): javax.ws.rs.core.Response {
        val url = URL(spec)
        if (!url.host.endsWith(".rackcdn.com")) {
            throw WebApplicationException("$spec proxy is forbidden", javax.ws.rs.core.Response.Status.FORBIDDEN)
        }
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.doInput = true
        conn.connect()
        if (conn.responseCode > 399)
            throw WebApplicationException(conn.responseMessage, conn.responseCode)

        return javax.ws.rs.core.Response.ok(
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
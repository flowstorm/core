package com.promethistai.port.servlets

import com.google.gson.Gson
import com.promethistai.common.AppConfig
import com.promethistai.port.AppClientManifest
import com.promethistai.port.Endpoints
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "Audio Stream Client Bootstrap", urlPatterns = ["/client/bootstrap"])
class AppManifestServlet : HttpServlet() {

    @Throws(ServletException::class)
    override fun doGet(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) {
        val manifest = AppClientManifest()
        val host = AppConfig.instance["service.host"]
        manifest.sttAudioInputStream = endpoint(httpRequest, "ws", host!!, Endpoints.AUDIO_INPUT_STREAM)
        manifest.ttsAudioOutput = endpoint(httpRequest, "http", host, Endpoints.AUDIO_OUTPUT)
        manifest.ttsVoice = endpoint(httpRequest, "http", host, Endpoints.VOICE)
        manifest.channel = endpoint(httpRequest, "ws", host, Endpoints.CHAT_CHANNEL)

        httpResponse.status = HttpServletResponse.SC_OK
        httpResponse.contentType = "application/json"
        httpResponse.setHeader("Access-Control-Allow-Origin", "*")
        try {
            httpResponse.writer.use { writer -> writer.write(Gson().toJson(manifest)) }
        } catch (e: IOException) {
            throw ServletException(e)
        }
    }

    internal fun endpoint(httpRequest: HttpServletRequest, endpointProtocol: String, endpointHost: String, endpointPath: String): String {
        return if ("localhost" == httpRequest.serverName)
            endpointProtocol + "://localhost:" + httpRequest.serverPort + endpointPath
        else
            endpointProtocol + "s://" + endpointHost + endpointPath
    }

}
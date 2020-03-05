package com.promethist.port.servlets

import com.promethist.common.ObjectUtil
import com.promethist.port.AppClientManifest
import com.promethist.port.Endpoints
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "Audio Stream Client Bootstrap", urlPatterns = ["/client/bootstrap"])
class AppManifestServlet : HttpServlet() {

    private val mapper = ObjectUtil.defaultMapper

    @Throws(ServletException::class)
    override fun doGet(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) {
        val manifest = AppClientManifest()
        var sec = ""
        var host = httpRequest.serverName
        if (httpRequest.serverName != "localhost") {
            sec = "s"
        } else if (httpRequest.serverPort != 80) {
            host += ":${httpRequest.serverPort}"
        }
        manifest.sttAudioInputStream = "ws${sec}://${host}${Endpoints.AUDIO_INPUT_STREAM}"
        manifest.ttsAudioOutput = "http${sec}://${host}${Endpoints.AUDIO_OUTPUT}"
        manifest.ttsVoice = "http${sec}://${host}${Endpoints.VOICE}"
        manifest.channel = "ws${sec}://${host}${Endpoints.CHAT_CHANNEL}"

        httpResponse.status = HttpServletResponse.SC_OK
        httpResponse.contentType = "application/json"
        httpResponse.setHeader("Access-Control-Allow-Origin", "*")
        try {
            httpResponse.writer.use { writer -> writer.write(mapper.writeValueAsString(manifest)) }
        } catch (e: IOException) {
            throw ServletException(e)
        }
    }

}
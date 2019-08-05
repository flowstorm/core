package com.promethistai.port.servlets

import com.promethistai.port.stt.SttWebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "STT WebSocket Servlet", urlPatterns = ["/stream/stt","/audio/input/stream/"/*bronzerabbit compatibility*/])
class SttServlet : WebSocketServlet() {

    override fun configure(factory: WebSocketServletFactory) {
        factory.register(SttWebSocket::class.java)
    }

}
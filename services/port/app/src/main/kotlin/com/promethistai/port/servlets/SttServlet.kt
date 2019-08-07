package com.promethistai.port.servlets

import com.promethistai.common.servlets.InjectableWebSocketServlet
import com.promethistai.port.stt.SttWebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "STT WebSocket Servlet", urlPatterns = ["/stream/stt","/audio/input/stream/"/*bronzerabbit compatibility*/])
class SttServlet : InjectableWebSocketServlet<SttWebSocket>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, SttWebSocket::class.java)
    }

}
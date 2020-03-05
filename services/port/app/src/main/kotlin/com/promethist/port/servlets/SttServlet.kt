package com.promethist.port.servlets

import com.promethist.common.servlets.InjectableWebSocketServlet
import com.promethist.port.stt.SttWebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "STT WebSocket Servlet", urlPatterns = ["/stream/stt","/input/stream/"/*bronzerabbit compatibility*/])
class SttServlet : InjectableWebSocketServlet<SttWebSocket>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, SttWebSocket::class.java)
    }

}
package ai.flowstorm.core.servlets

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import ai.flowstorm.common.servlets.InjectableWebSocketServlet
import ai.flowstorm.core.socket.BotCallSocketAdapter
import javax.servlet.annotation.WebServlet

@WebServlet(name = "BotCall WebSocket Servlet", urlPatterns = ["/call/"])
class BotCallServlet : InjectableWebSocketServlet<BotCallSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotCallSocketAdapter::class.java)
    }

}
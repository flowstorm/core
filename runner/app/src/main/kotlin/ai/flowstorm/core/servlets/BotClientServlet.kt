package ai.flowstorm.core.servlets

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import ai.flowstorm.common.servlets.InjectableWebSocketServlet
import ai.flowstorm.core.socket.BotClientSocketAdapter
import javax.servlet.annotation.WebServlet

@WebServlet(name = "BotClient WebSocket Servlet", urlPatterns = ["/socket/", "/client/"])
class BotClientServlet : InjectableWebSocketServlet<BotClientSocketAdapter>() {

    override fun configure(factory: WebSocketServletFactory) {
        configure(factory, BotClientSocketAdapter::class.java)
    }

}
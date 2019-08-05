package com.promethistai.port.servlets

import com.promethistai.port.bot.BotWebSocket
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.annotation.WebServlet

@WebServlet(name = "Bot WebSocket Servlet", urlPatterns = ["/stream/bot", "/channel/"/*bronzerabbit compatibility*/])
class BotServlet : WebSocketServlet() {

    override fun configure(factory: WebSocketServletFactory) {
        factory.register(BotWebSocket::class.java)
    }

}
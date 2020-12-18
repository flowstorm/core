package org.promethist.client.client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.promethist.client.BotEvent
import org.promethist.client.BotSocket
import org.promethist.client.common.BotClientSocket
import java.net.URI
import java.nio.ByteBuffer

class JwsBotClientSocket(url: String, raiseExceptions: Boolean = false, socketPing: Long = 10):
        BotClientSocket(url, raiseExceptions, socketPing) {

    private var client: WebSocketClient? = null

    override fun sendText(text: String) = client!!.send(text)

    override fun sendBytes(bytes: ByteBuffer) = client!!.send(bytes)

    override fun open() {
        client = object: WebSocketClient(URI(url.replace("http", "ws") + BotClientSocket.Companion.DEFAULT_URI)) {
            override fun onOpen(handshake: ServerHandshake?) {
                state = BotSocket.State.Open
                listener?.onOpen()
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                logger.info("onClose(code = $code, reason = $reason, remote = $remote)")
                state = BotSocket.State.Closed
                listener?.onClose()
            }

            override fun onMessage(text: String) {
                val event = objectMapper.readValue(text, BotEvent::class.java)
                listener?.onEvent(event)
            }

            override fun onError(ex: Exception) {
                logger.info("onError(ex = $ex)")
                state = BotSocket.State.Failed
                listener?.onFailure(ex)
            }

        }
        client!!.connect()
    }

    override fun close() {
        super.close()
        client?.close(1000, "CLIENT_CLOSE")
    }
}

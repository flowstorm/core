package org.promethist.client.common

import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.promethist.client.BotEvent
import org.promethist.client.BotSocket
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class OkHttp3BotClientSocket(url: String, raiseExceptions: Boolean = false, socketPing: Long = 10):
        BotClientSocket(url, raiseExceptions, socketPing) {

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            state = BotSocket.State.Open
            listener?.onOpen()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            logger.debug(text)
            val event = objectMapper.readValue(text, BotEvent::class.java)
            listener?.onEvent(event)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            logger.debug("Message ${bytes.size} bytes")
            listener?.onAudioData(bytes.toByteArray())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            logger.info("Closing (webSocket=$webSocket, code=$code, reason=$reason)")
            state = BotSocket.State.Closing
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            logger.info("Closed (webSocket=$webSocket, code=$code, reason=$reason)")
            state = BotSocket.State.Closed
            listener?.onClose()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            logger.info("Failure (webSocket=$webSocket, t=$t, reponse=$response)")
            state = BotSocket.State.Failed
            listener?.onFailure(t)
        }
    }

    private var socket: WebSocket? = null

    override fun open() {
        val url = url.replace("http", "ws") + DEFAULT_URI
        logger.info("Opening (url=$url)")
        val request = Request.Builder().url(url).build()
        val socketBuilder = OkHttpClient.Builder()
        if (socketPing > 0)
            socketBuilder.pingInterval(socketPing, TimeUnit.SECONDS)
        socket = socketBuilder.build().newWebSocket(request, socketListener)
    }

    override fun close() {
        super.close()
        socket!!.close(1000, "CLIENT_CLOSE")
    }


    override fun sendText(text: String) {
        socket!!.send(text)
    }

    override fun sendBytes(bytes: ByteBuffer) {
        socket!!.send(bytes.toByteString())
    }
}
package com.promethistai.port.stt

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import java.io.IOException

// todo still maintain this class?
class SttWebSocket : WebSocketAdapter() {

    private val gson = GsonBuilder().create()
    private var client: SttService? = null
    private var stream: SttStream? = null

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        super.onWebSocketBinary(payload, offset, len)
        stream?.write(payload, offset, len)
    }

    override fun onWebSocketText(message: String) {
        super.onWebSocketText(message)
        try {
            val command = gson.fromJson<Any>(message, SttCommand::class.java) as SttCommand
            when (command.type) {
                SttCommand.Type.Init -> {
                    if (command.params == null || command.params!!.isEmpty())
                        return
                    val language = command.params?.get("language") as String
                    val sampleRate = command.params?.get("sampleRate") as Double
                    client = SttServiceFactory.create("google", SttConfig(language, Math.round(sampleRate).toInt()), listOf(),
                        object : SttCallback {
                            override fun onResponse(transcript: String, confidence: Float, final: Boolean) {
                                val event = SttEvent()
                                event.type = SttEvent.Type.Recognized
                                event.params["transcript"] = transcript
                                event.params["confidence"] = confidence
                                event.params["isFinal"] = final
                                sendEvent(event)
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                                if (isNotConnected)
                                    return
                                val event = SttEvent()
                                event.type = SttEvent.Type.Error
                                event.params["message"] = e.message
                                sendEvent(event)
                            }

                            override fun onOpen() {
                                val event = SttEvent()
                                event.type = SttEvent.Type.Listening
                                sendEvent(event)
                            }
                        }
                    )
                    stream = client?.createStream()
                }

                SttCommand.Type.Pause -> if (stream != null) {
                    stream?.close()
                    stream = null
                }

                SttCommand.Type.Resume -> if (stream == null && client != null) {
                    stream = client?.createStream()
                }
            }
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        close()
    }

    override fun onWebSocketError(cause: Throwable?) {
        super.onWebSocketError(cause)
        close()
    }

    internal fun sendEvent(event: SttEvent) = try {
        remote.sendString(gson.toJson(event))
    } catch (e: IOException) {
        e.printStackTrace()
    }

    private fun close() {
        stream?.close()
        stream = null
        client?.close()
        client = null
    }
}
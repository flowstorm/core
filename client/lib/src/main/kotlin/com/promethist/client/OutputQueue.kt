package com.promethist.client

import java.util.*

class OutputQueue(val botClient: BotClient) : LinkedList<OutputQueue.Item>(), Runnable {

    open class Item(val type: Type) {

        enum class Type { Server, Local }

        class Text(val text: String, type: Type = Type.Server) : Item(type)
        class Audio(val data: ByteArray, type: Type = Type.Server) : Item(type)
        class Image(val url: String, type: Type = Type.Server): Item(type)
    }

    var suspended = false
    var running = false

    override fun run() {
        running = true
        while (running) {
            if (isNotEmpty() && !suspended) {
                val item = remove()
                with (botClient) {
                    when (item) {
                        is Item.Text -> text(item.text)
                        is Item.Audio -> audio(item.data)
                        is Item.Image -> image(item.url)
                    }
                    if (isEmpty()) {
                        if (!outputCancelled && (state != BotClient.State.Sleeping) && (item.type == Item.Type.Server)) {
                            inputAudioStreamOpen()
                        } else if (state == BotClient.State.Sleeping && (botClient.context.sessionId == null)) {
                            botClient.inputAudioRecorder?.stop()
                        }
                    }
                }
            } else {
                Thread.sleep(50)
            }
            if (botClient.outputCancelled) {
                synchronized (this) {
                    botClient.outputCancelled = false
                    for (item in LinkedList(this))
                        if (item.type == Item.Type.Server)
                            remove(item)
                }
            }
        }
    }

    fun close() {
        running = false
    }
}
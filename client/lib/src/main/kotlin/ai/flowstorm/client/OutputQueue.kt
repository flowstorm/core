package ai.flowstorm.client

import java.util.*

class OutputQueue(val client: BotClient) : Runnable {

    open class Item(val type: Type) {

        enum class Type { Server, Client }

        class Text(val text: String, type: Type = Type.Server) : Item(type)
        class Audio(val data: ByteArray, type: Type = Type.Server) : Item(type)
        class Image(val url: String, type: Type = Type.Server): Item(type)
    }

    var suspended = false
    var running = false
    private val items = LinkedList<Item>()

    override fun run() {
        running = true
        while (running) {
            if (items.isNotEmpty() && !suspended) {
                with (client) {
                    val item = items.remove()
                    when (item) {
                        is Item.Text -> text(item.text)
                        is Item.Audio -> audio(item.data)
                        is Item.Image -> image(item.url)
                    }
                    if (items.isEmpty()) {
                        if (state == BotClient.State.Failed && context.sessionId != null) {
                            endSession()
                        } else if ((state == BotClient.State.Responding) && (item.type == Item.Type.Server)) {
                            inputAudioStreamOpen()
                        } else if (state == BotClient.State.Sleeping && (context.sessionId == null)) {
                            inputAudioRecorder?.stop()
                        }
                    }
                }
            } else {
                Thread.sleep(50)
            }
        }
    }

    fun add(item: Item) = items.add(item)

    fun clear(type: Item.Type? = null) = synchronized (items) {
        for (item in LinkedList(items))
            if (type == null || type == item.type)
                items.remove(item)
    }

    fun close() {
        running = false
    }
}
package com.promethist.client

import com.promethist.client.BotClient
import java.util.*

class InputAudioQueue(val client: BotClient) : LinkedList<ByteArray>(), Runnable {

    override fun run() {
        while (true) {
            synchronized(this) {
                if (client.inputAudioStreamOpen && size > 0) {
                    val data = remove()
                    client.sendInputAudioData(data, data.size)
                } else {
                    Thread.sleep(50)
                }
            }
        }
    }
}
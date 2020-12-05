package org.promethist.client

interface BotSocket {

    enum class State { New, Open, Closing, Closed, Failed }

    interface Listener {

        fun onOpen()

        fun onClose()

        fun onEvent(event: BotEvent)

        fun onAudioData(data: ByteArray)

        fun onFailure(t: Throwable)
    }

    var state: State

    var listener: Listener?

    fun open()

    fun close()

    fun sendEvent(event: BotEvent)

    fun sendAudioData(data: ByteArray, count: Int? = null)
}
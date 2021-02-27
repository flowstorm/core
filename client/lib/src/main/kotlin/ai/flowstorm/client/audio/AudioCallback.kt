package ai.flowstorm.client.audio

interface AudioCallback {

    fun onStart()

    fun onData(data: ByteArray, size: Int): Boolean

    fun onWake()

    fun onStop()
}
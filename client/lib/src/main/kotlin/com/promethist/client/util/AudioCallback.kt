package com.promethist.client.util

interface AudioCallback {

    fun onStart()

    fun onData(data: ByteArray, size: Int): Boolean

    fun onStop()
}
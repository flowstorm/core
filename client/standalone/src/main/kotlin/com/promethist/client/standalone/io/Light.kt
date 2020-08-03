package com.promethist.client.standalone.io

interface Light {

    fun high()
    fun low()
    fun blink(ms: Long)
}
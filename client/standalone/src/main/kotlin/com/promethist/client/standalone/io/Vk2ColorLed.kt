package com.promethist.client.standalone.io

import java.awt.Color
import java.io.File

class Vk2ColorLed(index: Int = 1): ColorLight {

    val devicePath = "/sys/class/leds/ktd202x:led${index}/device/"
    var enabled = false

    private fun write(name: String, value: String) = File(devicePath, name).writeText(value)

    override fun set(color: Color) {
        write("registers", "led1=${color.red};led2=${color.green};led3=${color.blue};" + (
            if (!enabled) "ch1_enable=1;ch2_enable=1;ch3_enable=1;" else "")
        )
        enabled = true
    }
    override fun high() = set(Color.WHITE)

    override fun low() {
        write("registers", "ch1_enable=0;ch2_enable=0;ch3_enable=0;")
        enabled = false
    }

    override fun blink(ms: Long) {}
}
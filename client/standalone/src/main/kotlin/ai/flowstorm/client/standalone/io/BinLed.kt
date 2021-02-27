package ai.flowstorm.client.standalone.io

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.Pin
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin

class BinLed(gpio: GpioController, pin: Pin = RaspiPin.GPIO_06): Light {

    val outputPin = gpio.provisionDigitalOutputPin(pin, PinState.LOW)

    override fun high() = outputPin.high()

    override fun low() = outputPin.low()

    override fun blink(ms: Long) { outputPin.blink(ms) }
}
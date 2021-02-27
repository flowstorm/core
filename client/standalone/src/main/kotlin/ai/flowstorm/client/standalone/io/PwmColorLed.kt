package ai.flowstorm.client.standalone.io

import com.pi4j.io.gpio.*
import com.pi4j.wiringpi.SoftPwm
import java.awt.Color

class PwmColorLed(gpio: GpioController, val pinLayout: PinLayout = PinLayout.default): ColorLight {

    class PinLayout(val redPin: Pin, val greenPin: Pin, val bluePin: Pin) {

        companion object {

            val default = PinLayout(RaspiPin.GPIO_00, RaspiPin.GPIO_02, RaspiPin.GPIO_03)
        }

    }
    var color: Color = Color.BLACK

    init {
        val ledRed = gpio.provisionDigitalOutputPin(pinLayout.redPin)
        val ledGreen = gpio.provisionDigitalOutputPin(pinLayout.greenPin)
        val ledBlue = gpio.provisionDigitalOutputPin(pinLayout.bluePin)
        ledRed.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        ledGreen.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        ledBlue.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF)
        SoftPwm.softPwmCreate(pinLayout.redPin.address, 0, 50)
        SoftPwm.softPwmCreate(pinLayout.greenPin.address, 0, 50)
        SoftPwm.softPwmCreate(pinLayout.bluePin.address, 0, 50)
        low()
    }

    override fun set(color: Color) {
        val colors: FloatArray = color.getRGBColorComponents(null)
        SoftPwm.softPwmWrite(pinLayout.redPin.address, (colors[0] * 50f).toInt())
        SoftPwm.softPwmWrite(pinLayout.greenPin.address, (colors[1] * 50f).toInt())
        SoftPwm.softPwmWrite(pinLayout.bluePin.address, (colors[2] * 50f).toInt())
        this.color = color
    }

    override fun high() = set(Color.WHITE)

    override fun low() = set(Color.BLACK)

    override fun blink(ms: Long) { TODO("") }
}
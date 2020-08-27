package com.promethist.client.standalone.io

import com.promethist.client.signal.SignalProcessor
import com.promethist.client.signal.SignalProvider
import com.promethist.client.audio.SpeechDevice
import org.usb4java.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.or

object RespeakerMicArrayV2 : SpeechDevice, SignalProvider {

    private const val VENDOR_ID = 0x2886.toShort()
    private const val PRODUCT_ID = 0x0018.toShort()
    private const val DOA_PARAMETER_ID: Short = 21
    private const val DOA_PARAMETER_OFFSET: Short = 0
    private const val VOICE_ACTIVITY_PARAMETER_ID: Short = 19
    private const val VOICE_ACTIVITY__PARAMETER_OFFSET: Short = 32

    override lateinit var processor: SignalProcessor
    private val context = Context()
    private val deviceHandle by lazy {
        var result = LibUsb.init(context)
        if (result != LibUsb.SUCCESS)
            throw LibUsbException("Unable to initialize USB lib", result)
        val device: Device? = findDevice(context, VENDOR_ID, PRODUCT_ID)
        val handle = DeviceHandle()
        result = LibUsb.open(device, handle)
        if (result != LibUsb.SUCCESS)
            throw LibUsbException("Unable to open USB device", result)
        handle
    }
    private var lastSpeechTime = 0L

    override val isSpeechDetected: Boolean get() {
        val currentTime = System.currentTimeMillis()
        return if (read(deviceHandle, VOICE_ACTIVITY_PARAMETER_ID, VOICE_ACTIVITY__PARAMETER_OFFSET) == 1) {
            lastSpeechTime = currentTime
            true
        } else {
            lastSpeechTime + 250 > currentTime
        }
    }

    override val speechAngle get() = read(deviceHandle, DOA_PARAMETER_ID, DOA_PARAMETER_OFFSET)

    override fun close() {
        LibUsb.close(deviceHandle)
        LibUsb.exit(context)
    }

    override fun run() {
        while (true) {
            processor.process(mapOf(
                    "clientSpeechDetected" to isSpeechDetected,
                    "clientSpeechAngle" to speechAngle
            ))
            Thread.sleep(500)
        }
    }

    fun test() {
        var i = 0
        while (i < 1000) {
            println("angle = $speechAngle\tspeech = $isSpeechDetected")
            i++
            Thread.sleep(50)
        }
        close()
    }

    fun findDevice(context: Context?, vendorId: Short, productId: Short): Device? {
        // Read the USB device list
        val list = DeviceList()
        var result: Int = LibUsb.getDeviceList(context, list)
        if (result < 0) {
            throw LibUsbException("Unable to get device list", result)
        }
        try {
            // Iterate over all devices and scan for the right one
            for (device in list) {
                val descriptor = DeviceDescriptor()
                result = LibUsb.getDeviceDescriptor(device, descriptor)
                if (result != LibUsb.SUCCESS) {
                    throw LibUsbException("Unable to read device descriptor", result)
                }
                if (descriptor.idVendor() === vendorId && descriptor.idProduct() === productId) {
                    return device
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true)
        }

        // Device not found
        return null
    }

    private fun read(deviceHandle: DeviceHandle?, id: Short, offset: Short): Int {
        val cmd = (0x80.toShort() or offset or 0x40.toShort())
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(8)
        val transfered: Int = LibUsb.controlTransfer(deviceHandle,
                (LibUsb.ENDPOINT_IN or LibUsb.REQUEST_TYPE_VENDOR or LibUsb.RECIPIENT_DEVICE),
                0.toByte(), cmd, id, buffer, 5000L)
        if (transfered < 0) {
            throw LibUsbException("Control transfer failed", transfered)
        }
        return buffer.order(ByteOrder.LITTLE_ENDIAN).getInt(0)
    }

    override fun toString(): String = this::class.simpleName!!
}
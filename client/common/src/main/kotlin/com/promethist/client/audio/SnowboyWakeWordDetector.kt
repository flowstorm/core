package com.promethist.client.audio

import ai.kitt.snowboy.SnowboyDetect
import com.promethist.util.LoggerDelegate
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SnowboyWakeWordDetector(config: WakeWordConfig, bufferSize: Int) : WakeWordDetector {

    private val logger by LoggerDelegate()
    private val detector: SnowboyDetect
    private val wakeWordBuffer = ShortArray(bufferSize)

    init {
        val tempDir = System.getProperty("java.io.tmpdir")
        val osName = System.getProperty("os.name").replace(" ", "").toLowerCase()
        val osArch = System.getProperty("os.arch")
        val libName = "snowboy-$osName-$osArch." + when (osName) {
            "linux" -> "so"
            "macosx" -> "dylib"
            "windows" -> "dll"
            else -> error("unsupported OS $osName")
        }
        val libFile = if (config.libFile != null)
            File(config.dir, config.libFile)
        else
            File(tempDir, libName)
        listOf("snowboy.res", "snowboy.umdl", libName).forEach { name ->
            FileOutputStream(File(tempDir, name)).use {
                javaClass.getResourceAsStream("/$name").copyTo(it)
            }
        }
        val resFile = File(config.dir, "snowboy.res").run {
            if (exists()) this else File(tempDir, "snowboy.res")
        }
        val modelFile = File(config.dir, "snowboy.umdl").run {
            if (exists()) this else File(tempDir, "snowboy.umdl")
        }

        logger.info("loading $libFile, resFile = $resFile, modelFile = $modelFile, wakeWordBuffer.size = ${wakeWordBuffer.size}")
        System.load(libFile.absolutePath)
        detector = SnowboyDetect(resFile.absolutePath, modelFile.absolutePath).apply {
            SetSensitivity(config.sensitivity.toString())
            SetAudioGain(config.gain)
            ApplyFrontend(false)
        }
    }

    override fun detect(buffer: ByteArray, count: Int): Boolean {
        ByteBuffer.wrap(buffer, 0, count).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(wakeWordBuffer)
        val result = detector.RunDetection(wakeWordBuffer, count / 2)
        return result > 0
    }
}
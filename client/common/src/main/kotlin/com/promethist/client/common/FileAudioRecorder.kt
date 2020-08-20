package com.promethist.client.common

import com.promethist.client.HttpRequest
import com.promethist.client.util.AudioRecorder
import com.promethist.client.util.HttpUtil
import com.promethist.util.DataConverter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalTime

class FileAudioRecorder(val dir: File, val filestoreUrl: String, val uploadMode: UploadMode = UploadMode.night) : AudioRecorder, Runnable {

    enum class UploadMode { none, local, night, immediate }

    override var outputStream: OutputStream? = null
    var sessionId: String? = null
    var pcmFile: File? = null
    var wavFile: File? = null

    init {
        if (uploadMode != UploadMode.none)
            Thread(this).start()
    }

    override fun start(sessionId: String) {
        this.sessionId = sessionId
        pcmFile = File(dir, "$sessionId.pcm")
        wavFile = File(dir, "$sessionId.wav")
        outputStream = FileOutputStream(pcmFile)
        println("{Recording $pcmFile}")
    }

    override fun stop() {
        if (outputStream != null) {
            outputStream!!.close()
            outputStream = null
            if (pcmFile!!.exists()) {
                val length = pcmFile!!.length()
                FileInputStream(pcmFile).use { input ->
                    println("{Copying $length bytes from .pcm to $wavFile}")
                    FileOutputStream(wavFile).use { output ->
                        DataConverter.pcmToWav(input, output, length)
                    }
                }
                pcmFile?.delete()
            }
        }
    }

    override fun run() {
        while (true) {
            val now = LocalTime.now()
            if ((uploadMode == UploadMode.immediate) || ((uploadMode == UploadMode.night) && (now.hour in 3..5))) {
                try {
                    dir.walk().maxDepth(1).forEach { file ->
                        if (file.extension == "wav") {
                            val fileUrl = "$filestoreUrl/session/${file.name}"
                            println("{Uploading $file to $fileUrl}")
                            //TODO streaming upload
                            HttpUtil.httpRequest(fileUrl,
                                    HttpRequest("POST", "audio/wav", emptyMap(), file.readBytes()))
                            file.delete()
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            Thread.sleep(5000)
        }
    }
}
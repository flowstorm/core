package ai.flowstorm.client.audio

import ai.flowstorm.client.HttpRequest
import ai.flowstorm.client.util.HttpUtil
import ai.flowstorm.util.DataConverter
import java.io.File
import java.io.RandomAccessFile
import java.time.LocalTime

class WavFileAudioRecorder(val dir: File, val filestoreUrl: String, val uploadMode: UploadMode = UploadMode.night) : AudioRecorder, Runnable {

    enum class UploadMode { none, local, night, immediate }

    var file: File? = null
    var sessionId: String? = null

    init {
        if (uploadMode != UploadMode.none)
            Thread(this).start()
    }

    override fun start(sessionId: String) {
        this.sessionId = sessionId
        file = File(dir, "$sessionId.wav")
        println("{Starting recording $file}")
    }

    override fun write(data: ByteArray) {
        if (file != null) {
            RandomAccessFile(file, "rws").apply {
                val length = length()
                if (length == 0L) {
                    write(DataConverter.wavHeader(0))
                } else {
                    seek(length)
                    write(data)
                    seek(40)
                    writeInt(length.toInt() + data.size - 44)
                }
                close()
            }
        }
    }

    override fun stop() {
        println("{Stopping recording $file}")
        file = null
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
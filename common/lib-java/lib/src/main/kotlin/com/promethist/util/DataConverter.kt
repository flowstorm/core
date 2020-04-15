package com.promethist.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object DataConverter {

    /**
     * Write PCM data as WAV file
     * @param os  Stream to save file to
     * @param pcmdata  8 bit PCMData
     * @param sampleRate  Sample rate - 8000, 16000, etc.
     * @param channels Number of channels - Mono = 1, Stereo = 2, etc..
     * @param sampleSizeInBits Number of bits per sample (16 here)
     * @throws IOException
     */
    @Throws(IOException::class)
    fun pcmToWav(input: InputStream, output: OutputStream, size: Long, sampleRate: Int = 16000, channels: Int = 1, sampleSizeInBits: Int = 16) {
        val header = ByteArray(44)
        //val data = get16BitPcm(pcmdata)

        val totalDataLen = (size + 36)
        val bitrate = (sampleRate * channels * sampleSizeInBits).toLong()

        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = sampleSizeInBits.toByte()
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (bitrate / 8 and 0xff).toByte()
        header[29] = (bitrate / 8 shr 8 and 0xff).toByte()
        header[30] = (bitrate / 8 shr 16 and 0xff).toByte()
        header[31] = (bitrate / 8 shr 24 and 0xff).toByte()
        header[32] = (channels * sampleSizeInBits / 8).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (size and 0xff).toByte()
        header[41] = (size shr 8 and 0xff).toByte()
        header[42] = (size shr 16 and 0xff).toByte()
        header[43] = (size shr 24 and 0xff).toByte()

        output.write(header, 0, 44)
        input.copyTo(output)
    }

    fun valueFromString(name: String, type: String, str: String) = when (type) {
        Int::class.simpleName -> str.toInt()
        Long::class.simpleName -> str.toLong()
        Float::class.simpleName -> str.toFloat()
        Double::class.simpleName -> str.toDouble()
        Boolean::class.simpleName -> str.toBoolean()
        String::class.simpleName -> str
        else -> error("Property $name has unsupported type $type")
    }
}

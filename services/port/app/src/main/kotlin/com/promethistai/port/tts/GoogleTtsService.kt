package com.promethistai.port.tts.impl

import com.google.cloud.texttospeech.v1beta1.*
import com.promethistai.port.tts.TtsConfig
import com.promethistai.port.tts.TtsRequest
import com.promethistai.port.tts.TtsService
import com.promethistai.port.tts.TtsVoice
import java.util.*
import java.util.concurrent.TimeUnit

class GoogleTtsService : TtsService {

    private val client = TextToSpeechClient.create()

    override val voices: List<TtsVoice>
        get() {
            val request = ListVoicesRequest.getDefaultInstance()
            val voices = mutableListOf<TtsVoice>()
            for (voice in client.listVoices(request).voicesList)
                voices.add(
                    TtsVoice(voice.name, voice.ssmlGender.name,
                        if (voice.languageCodesCount > 0) voice.getLanguageCodes(0) else "undef"))
            return voices
        }

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        // Set the text input to be synthesized
        val input : SynthesisInput = if (ttsRequest.isSsml) {
            SynthesisInput.newBuilder().setSsml(ttsRequest.text).build()
        } else {
            SynthesisInput.newBuilder().setText(ttsRequest.text).build()
        }

        // Build the voice request, select the language code ("en-US") and the ssml voice gender ("neutral")
        val voice = VoiceSelectionParams.newBuilder()
                .setName(ttsRequest.voice)
                .setLanguageCode(ttsRequest.language)
                //.setSsmlGender(SsmlVoiceGender.MALE)
                .build()

        // Select the type of stream file you want returned
        val audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .setSpeakingRate(ttsRequest.speakingRate)
                .setPitch(ttsRequest.pitch)
                .setVolumeGainDb(ttsRequest.volumeGain)
                .build()

        // Perform the text-to-speech request on the text input with the selected voice parameters and stream file type
        val response = client.synthesizeSpeech(input, voice, audioConfig)

        // Get the stream contents from the response
        val audioContents = response.audioContent

        return audioContents.toByteArray()
    }

    override fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        client.close()
    }

}
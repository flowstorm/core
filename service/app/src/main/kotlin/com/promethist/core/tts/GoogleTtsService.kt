package com.promethist.core.tts

import com.google.cloud.texttospeech.v1beta1.*
import com.promethist.core.model.TtsConfig
import io.sentry.Sentry

object GoogleTtsService: TtsService {

    private val client = TextToSpeechClient.create()

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        // Set the text input to be synthesized
        val input : SynthesisInput = if (ttsRequest.isSsml) {
            SynthesisInput.newBuilder().setSsml(ttsRequest.text).build()
        } else {
            SynthesisInput.newBuilder().setText(ttsRequest.text).build()
        }

        // Build the voice request, select the language code ("en-US") and the ssml voice gender ("neutral")
        val config = TtsConfig.forVoice(ttsRequest.voice)
        val voice = VoiceSelectionParams.newBuilder()
                .setName(config.name)
                .setLanguageCode(config.locale.toLanguageTag())
                //.setSsmlGender(SsmlVoiceGender.MALE)
                .build()

        // Select the type of stream file you want returned
        val audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .setSampleRateHertz(ttsRequest.sampleRate)
                .setSpeakingRate(ttsRequest.speakingRate)
                .setPitch(ttsRequest.speakingPitch)
                .setVolumeGainDb(ttsRequest.speakingVolumeGain)
                .build()

        TtsServiceFactory.logger.info("speak(ttsRequest = $ttsRequest)")
        // Perform the text-to-speech request on the text input with the selected voice parameters and stream file type
        val response = client.synthesizeSpeech(input, voice, audioConfig)

        // Get the stream contents from the response
        val audioContents = response.audioContent

        return audioContents.toByteArray()
    }

    fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            Sentry.captureException(e)
            e.printStackTrace()
        }
        client.close()
    }

}
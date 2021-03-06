package ai.flowstorm.core.tts

import ai.flowstorm.util.LoggerDelegate
import com.google.cloud.texttospeech.v1.*
import io.sentry.Sentry

class GoogleTtsProvider: TtsProvider {

    override val name = "Google"
    private val logger by LoggerDelegate()
    private val client = TextToSpeechClient.create()

    override fun speak(ttsRequest: TtsRequest): ByteArray {
        // Set the text input to be synthesized
        val input : SynthesisInput = if (ttsRequest.isSsml) {
            SynthesisInput.newBuilder().setSsml(ttsRequest.text).build()
        } else {
            SynthesisInput.newBuilder().setText(ttsRequest.text).build()
        }

        // Build the voice request, select the language code ("en-US") and the ssml voice gender ("neutral")
        val voice = VoiceSelectionParams.newBuilder()
                .setName(ttsRequest.config.name)
                .setLanguageCode(ttsRequest.config.locale.toLanguageTag())
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

        logger.info("speak ttsRequest=$ttsRequest")
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
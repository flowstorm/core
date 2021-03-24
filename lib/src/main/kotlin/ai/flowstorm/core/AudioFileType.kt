package ai.flowstorm.core

enum class AudioFileType(val contentType: String) {
    mp3("audio/mpeg"),
    wav("audio/wav"),
    mulaw("audio/basic")
}
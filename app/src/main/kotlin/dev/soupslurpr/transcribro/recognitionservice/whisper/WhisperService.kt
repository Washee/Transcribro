package dev.soupslurpr.transcribro.recognitionservice.whisper

interface WhisperService {
    /** A cheap reachability check used before an utterance starts. */
    suspend fun isAvailable(): Boolean = true

    suspend fun startTranscription(lang: String)
    suspend fun transcribeAudio(data: ShortArray): String
    suspend fun endTranscription(): String
    suspend fun release()
}

class WhisperUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

package dev.soupslurpr.transcribro.recognitionservice.whisper

interface WhisperService {
    suspend fun startTranscription(lang: String)
    suspend fun transcribeAudio(data: ShortArray): String
    suspend fun endTranscription(): String
    suspend fun release()
}
package dev.soupslurpr.transcribro.recognitionservice.whisper

import android.util.Log
import kotlinx.coroutines.CancellationException

/**
 * Tries [primary] first and replays the buffered utterance through [fallback] when the
 * primary backend cannot be reached. Audio is kept for the current utterance only.
 */
class FallbackWhisperService(
    private val primary: WhisperService,
    private val fallback: WhisperService,
    private val logTag: String = "WhisperFallback",
) : WhisperService {
    private var language = "de"
    private var audio = ShortArray(0)
    private var usingFallback = false
    private var fallbackStarted = false
    private var primaryText = ""

    override suspend fun isAvailable(): Boolean = true

    override suspend fun startTranscription(lang: String) {
        language = lang
        audio = ShortArray(0)
        primaryText = ""
        fallbackStarted = false
        usingFallback = !isReachable(primary)

        if (usingFallback) {
            Log.w(logTag, "Primary backend is unreachable; using fallback")
            startFallback(lang)
            return
        }

        try {
            primary.startTranscription(lang)
        } catch (exception: WhisperUnavailableException) {
            Log.w(logTag, "Primary backend failed to start; using fallback", exception)
            switchToFallback(lang)
        }
    }

    override suspend fun transcribeAudio(data: ShortArray): String {
        audio = data

        if (usingFallback) {
            return fallback.transcribeAudio(data)
        }

        return try {
            primaryText = primary.transcribeAudio(data)
            primaryText
        } catch (exception: WhisperUnavailableException) {
            Log.w(logTag, "Primary backend failed while sending audio; using fallback", exception)
            switchToFallback(language)
            fallback.transcribeAudio(data)
        }
    }

    override suspend fun endTranscription(): String {
        if (usingFallback) {
            return fallback.endTranscription()
        }

        return try {
            primaryText + primary.endTranscription()
        } catch (exception: WhisperUnavailableException) {
            Log.w(logTag, "Primary backend failed before returning a transcript; using fallback", exception)
            switchToFallback(language)
            fallback.transcribeAudio(audio) + fallback.endTranscription()
        }
    }

    override suspend fun release() {
        primary.release()
        fallback.release()
    }

    private suspend fun isReachable(service: WhisperService): Boolean {
        return try {
            service.isAvailable()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(logTag, "Availability check failed; treating backend as unreachable", exception)
            false
        }
    }

    private suspend fun switchToFallback(lang: String) {
        usingFallback = true
        startFallback(lang)
    }

    private suspend fun startFallback(lang: String) {
        if (!fallbackStarted) {
            fallback.startTranscription(lang)
            fallbackStarted = true
        }
    }
}

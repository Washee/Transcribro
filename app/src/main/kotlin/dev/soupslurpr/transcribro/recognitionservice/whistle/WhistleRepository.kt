package dev.soupslurpr.transcribro.recognitionservice.whistle

import android.content.Context
import android.util.Log
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperService
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * On-device speech recognition through Cactus Whistle. The 16.9 MB model is bundled in
 * the APK and runs entirely on the CPU; no network permission is needed for transcription.
 */
class WhistleRepository(private val context: Context) : WhisperService {
    companion object {
        private const val TAG = "WhistleRepository"
        private const val MODEL_ASSET = "models/whistle/whistle.cact"
    }

    private val mutex = Mutex()
    private var modelLoaded = false
    private var language = "de"

    override suspend fun isAvailable(): Boolean = CactusWhistleNative.isAvailable()

    override suspend fun startTranscription(lang: String) {
        language = lang
    }

    override suspend fun transcribeAudio(data: ShortArray): String = mutex.withLock {
        if (data.isEmpty()) {
            return ""
        }

        try {
            ensureModelLoaded()
            val pcm = FloatArray(data.size) { index ->
                (data[index] / 32768.0f).coerceIn(-1f..1f)
            }
            val response = withContext(Dispatchers.IO) {
                CactusWhistleNative.transcribe(pcm, language, wordTimestamps = false)
            }
            JSONObject(response).optString("text").trim()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e(TAG, "Whistle transcription failed", exception)
            throw WhisperUnavailableException("Whistle transcription failed", exception)
        }
    }

    override suspend fun endTranscription(): String = ""

    override suspend fun release() {
        // The Cactus runtime keeps one process-global model for the lifetime of the process.
    }

    private suspend fun ensureModelLoaded() {
        if (modelLoaded) {
            return
        }

        withContext(Dispatchers.IO) {
            val model = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            CactusWhistleNative.loadModel(model)
        }
        modelLoaded = true
    }
}

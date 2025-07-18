package dev.soupslurpr.transcribro.recognitionservice.whisper.local

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.whispercpp.whisper.WhisperContext
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperService

class WhisperRepository (
    private val whisperLocalDataSource: WhisperLocalDataSource
) : WhisperService {

    private var whisperContext: MutableState<WhisperContext?> =
        mutableStateOf(null)

    private suspend fun loadWhisperContextIfNull() {
        if (whisperContext.value == null) {
            whisperContext.value = whisperLocalDataSource.getWhisperContext()
        }
    }

    override suspend fun transcribeAudio(data: ShortArray): String {
        loadWhisperContextIfNull()
        // assume we only have one channel
        var buffer = FloatArray(data.size) { index ->
            (data[index] / 32767.0f).coerceIn(-1f..1f)
        }

        if (data.size < 32000) {
            val newBuffer = FloatArray(32000)

            for ((i, value) in buffer.withIndex()) {
                newBuffer[i] = value
            }

            newBuffer.fill(0f, data.size, newBuffer.size)

            buffer = newBuffer
        }

        val transcript = whisperContext.value?.transcribeData(buffer, ((data.size / 16000f) * 1000f).toLong()) ?: ""
        return transcript.removeSuffix(" .") // remove hallucination
    }

    override suspend fun release() {
        whisperContext.value?.release()
    }

    override suspend fun endTranscription(): String {
        //nothing to do here
        return ""
    }

    override suspend fun startTranscription(lang: String) {
        //do nothing
    }
}
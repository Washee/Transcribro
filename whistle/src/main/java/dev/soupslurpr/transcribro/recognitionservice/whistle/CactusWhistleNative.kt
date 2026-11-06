package dev.soupslurpr.transcribro.recognitionservice.whistle

import androidx.annotation.Keep

/**
 * Thin JNI bridge to the Cactus runtime and the Whistle speech model.
 *
 * The Cactus runtime owns one process-global model, so every native call is serialized.
 */
@Keep
object CactusWhistleNative {
    private var loadAttempted = false
    private var available = false
    private var loadError: String? = null

    fun isAvailable(): Boolean {
        if (!loadAttempted) {
            loadAttempted = true
            available = try {
                System.loadLibrary("cactus_jni")
                true
            } catch (throwable: Throwable) {
                loadError = throwable.message
                false
            }
        }
        return available
    }

    @Synchronized
    fun loadModel(model: ByteArray) {
        checkAvailable()
        val result = nativeLoad(model)
        if (result < 0) {
            error(nativeLastError())
        }
    }

    @Synchronized
    fun transcribe(
        pcm: FloatArray,
        language: String?,
        wordTimestamps: Boolean = false,
    ): String {
        checkAvailable()
        return nativeTranscribe(pcm, language, wordTimestamps)
    }

    private fun checkAvailable() {
        check(isAvailable()) {
            loadError ?: "The Cactus native library is not available for this device"
        }
    }

    private external fun nativeLoad(model: ByteArray): Int

    private external fun nativeTranscribe(
        pcm: FloatArray,
        language: String?,
        wordTimestamps: Boolean,
    ): String

    private external fun nativeLastError(): String
}

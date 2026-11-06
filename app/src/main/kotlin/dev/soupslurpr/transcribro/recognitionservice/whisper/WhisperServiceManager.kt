package dev.soupslurpr.transcribro.recognitionservice.whisper

import android.content.Context
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import dev.soupslurpr.transcribro.dataStore
import dev.soupslurpr.transcribro.preferences.PreferencesUiState
import dev.soupslurpr.transcribro.recognitionservice.whisper.local.WhisperApi
import dev.soupslurpr.transcribro.recognitionservice.whisper.local.WhisperLocalDataSource
import dev.soupslurpr.transcribro.recognitionservice.whisper.local.WhisperRepository
import dev.soupslurpr.transcribro.recognitionservice.whisper.wyoming.WyomingWhisperService
import dev.soupslurpr.transcribro.recognitionservice.whistle.CactusWhistleNative
import dev.soupslurpr.transcribro.recognitionservice.whistle.WhistleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

object WhisperServiceManager {
    private const val TAG = "WhisperServiceManager"

    private data class ServiceConfig(
        val useWyoming: Boolean,
        val address: String,
        val port: Int,
        val ssl: Boolean,
        val localFallback: Boolean,
    )

    private var currentConfig: ServiceConfig? = null
    private var whisperService: WhisperService? = null

    suspend fun getWhisperService(context: Context): WhisperService {
        val preferencesUiState = PreferencesUiState()
        val preferences = context.dataStore.data.first()

        val useWyoming = preferences[preferencesUiState.useWyoming.first]
            ?: preferencesUiState.useWyoming.second.value
        val address = preferences[preferencesUiState.wyomingAddress.first]
            ?: preferencesUiState.wyomingAddress.second.value
        val port = preferences[preferencesUiState.wyomingPort.first]
            ?: preferencesUiState.wyomingPort.second.value
        val ssl = preferences[preferencesUiState.wyomingSSL.first]
            ?: preferencesUiState.wyomingSSL.second.value
        val localFallback = preferences[preferencesUiState.useLocalWhisperFallback.first]
            ?: preferencesUiState.useLocalWhisperFallback.second.value

        val config = ServiceConfig(useWyoming, address, port, ssl, localFallback)

        if (whisperService == null || currentConfig != config) {
            Log.i(TAG, "Initialize Whisper service: $config")
            currentConfig = config

            val localService = createLocalService(context)
            whisperService = when {
                useWyoming && localFallback -> FallbackWhisperService(
                    primary = WyomingWhisperService(address, port, ssl),
                    fallback = localService,
                    logTag = "WyomingWhisperFallback",
                )

                useWyoming -> WyomingWhisperService(address, port, ssl)

                else -> localService
            }
        }

        return whisperService!!
    }

    private fun createLocalService(context: Context): WhisperService {
        val whisperService = WhisperRepository(
            WhisperLocalDataSource(
                whisperApi = object : WhisperApi {
                    override fun getWhisperContext(): WhisperContext {
                        return WhisperContext.createContextFromAsset(
                            context.assets,
                            "models/whisper/ggml-model-whisper-tiny.en-q8_0.bin",
                        )
                    }
                },
                ioDispatcher = Dispatchers.IO,
            ),
        )

        if (!CactusWhistleNative.isAvailable()) {
            Log.i(TAG, "Whistle is unavailable for this ABI; falling back to whisper.cpp")
            return whisperService
        }

        return FallbackWhisperService(
            primary = WhistleRepository(context.applicationContext),
            fallback = whisperService,
            logTag = "WhistleFallback",
        )
    }
}

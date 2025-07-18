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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

object WhisperServiceManager {
    private var currentUseWyoming: Boolean? = null
    private var whisperService: WhisperService? = null

    suspend fun getWhisperService(context: Context): WhisperService {
        val prefsUiState = PreferencesUiState()
        val preferences = context.dataStore.data.first()

        val useWyoming = preferences[prefsUiState.useWyoming.first] ?: prefsUiState.useWyoming.second.value

        if (whisperService == null || currentUseWyoming != useWyoming) {
            Log.i("WhisperServiceManager", "Initialize Whisper Service...")
            currentUseWyoming = useWyoming
            whisperService = if (useWyoming) {
                val wyomingAddress = preferences[prefsUiState.wyomingAddress.first] ?: prefsUiState.wyomingAddress.second.value
                val wyomingPort = preferences[prefsUiState.wyomingPort.first] ?: prefsUiState.wyomingPort.second.value
                val wyomingSSL = preferences[prefsUiState.wyomingSSL.first] ?: prefsUiState.wyomingSSL.second.value
                WyomingWhisperService(context, wyomingAddress, wyomingPort, wyomingSSL)
            } else {
                WhisperRepository(
                    WhisperLocalDataSource(
                        whisperApi = object : WhisperApi {
                            override fun getWhisperContext(): WhisperContext {
                                return WhisperContext.createContextFromAsset(
                                    context.assets,
                                    "models/whisper/ggml-model-whisper-tiny.en-q8_0.bin"
                                )
                            }
                        },
                        ioDispatcher = Dispatchers.IO,
                    )
                )
            }
        }
        return whisperService!!
    }
}
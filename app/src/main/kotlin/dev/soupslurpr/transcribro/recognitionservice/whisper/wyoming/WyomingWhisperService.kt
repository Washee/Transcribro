package dev.soupslurpr.transcribro.recognitionservice.whisper.wyoming

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class WyomingWhisperService (
    private val context: Context,
    private val host: String = "localhost",
    private val port: Int = 10300,
    private val ssl: Boolean = true
) : WhisperService {
    private lateinit var socket: Socket
    private lateinit var output: OutputStream
    private lateinit var input: InputStream

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun estabilshConnection(): Boolean {
        if (!isNetworkAvailable()) {
            Log.e("wyoming", "Kein Netzwerk verfügbar")
            return false
        }
        if(!::socket.isInitialized || socket.isClosed) {
            try {
                val address = InetAddress.getByName(host)
                Log.w("wyoming", address.toString())
                if(ssl) {
                    val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    socket = factory.createSocket(host, port)
                    // Optional: bestimmte Protokolle oder Ciphers erzwingen
                    (socket as SSLSocket).setEnabledProtocols(arrayOf("TLSv1.2"));
                } else {
                    socket = Socket(address, port)
                }

                output = socket.getOutputStream()
                input = socket.getInputStream()
                return true
            } catch (e: UnknownHostException) {
                Log.e("Socket", "DNS-Auflösung fehlgeschlagen", e)
                return false
            } catch (e: IOException) {
                Log.e("Socket", "Fehler beim Verbindungsaufbau", e)
                return false
            }
        } else {
            Log.w("Socket", "Socket ist noch offen sagt er.")
        }
        return true
    }

    private fun sendEvent(
        header: JSONObject,
        data: JSONObject? = null,
        payload: ByteArray? = null
    ) {
        val headerCopy = JSONObject(header.toString()) // clone
        val dataBytes = data?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val payloadBytes = payload ?: ByteArray(0)

        if (data != null) headerCopy.put("data_length", dataBytes.size)
        if (payload != null) headerCopy.put("payload_length", payloadBytes.size)

        // 1. Header mit \n
        val headerString = headerCopy.toString() + "\n"
        val headerBytes = headerString.toByteArray(Charsets.UTF_8)
        output.write(headerBytes)

        // 2. Optional: JSON-Daten
        if (dataBytes.isNotEmpty()) output.write(dataBytes)

        // 3. Optional: Payload (z. B. PCM)
        if (payloadBytes.isNotEmpty()) output.write(payloadBytes)

        output.flush()

        Log.d("wyoming", "Data JSON: ${data?.toString()}")
        //Log.d("wyoming", "data_length = ${dataBytes.size}")
        //Log.d("wyoming", "payload_length = ${payloadBytes.size}")
    }

    override suspend fun startTranscription(language: String) = withContext(Dispatchers.IO) {
        Log.d("wyoming", "startTranscription called from thread: ${Thread.currentThread().name}")

        if (!estabilshConnection())
            return@withContext

        Log.w("wyoming", "Starting Transcription")
        sendEvent(
            header = JSONObject(mapOf("type" to "transcribe", "version" to "1.7.1")),
            data = JSONObject(mapOf("language" to language))
        )

        val audioMeta = JSONObject(
            mapOf(
                "rate" to 16000,
                "width" to 2,
                "channels" to 1,
                "timestamp" to JSONObject.NULL
            )
        )

        sendEvent(
            header = JSONObject(mapOf("type" to "audio-start", "version" to "1.7.1")),
            data = audioMeta
        )
    }

    override suspend fun transcribeAudio(audioData: ShortArray): String =
        withContext(Dispatchers.IO) {
            if (!estabilshConnection())
                return@withContext ""

            Log.w("wyoming", "transcribe")

            val chunkSizeSamples = 4000  // 250 ms @ 16kHz
            var offset = 0
            var timestampMs = 0

            while (offset < audioData.size) {
                val end = minOf(offset + chunkSizeSamples, audioData.size)
                val chunkSamples = audioData.sliceArray(offset until end)
                val payload = shortArrayToLittleEndianByteArray(chunkSamples)

                val audioMeta = JSONObject(
                    mapOf(
                        "rate" to 16000,
                        "width" to 2,
                        "channels" to 1,
                        "timestamp" to timestampMs  // ✅ relativer Versatz in der Aufnahme
                    )
                )

                sendEvent(
                    header = JSONObject(mapOf("type" to "audio-chunk", "version" to "1.7.1")),
                    data = audioMeta,
                    payload = payload
                )

                offset = end
                // Zeit berechnen anhand tatsächlicher Chunk-Länge
                val chunkDurationMs =
                    (chunkSamples.size * 1000) / 16000  // z. B. 4000 Samples → 250 ms
                timestampMs += chunkDurationMs
            }

            return@withContext ""
        }

    override suspend fun endTranscription(): String = withContext(Dispatchers.IO) {
        if (!estabilshConnection())
            return@withContext ""

        Log.w("wyoming", "Ending Transcription")
        sendEvent(
            header = JSONObject(mapOf("type" to "audio-stop", "version" to "1.7.1")),
            data = JSONObject() // leeres JSON
        )

        // Ergebnis auslesen
        val result = readTranscriptionResult()
        Log.w("wyoming", "response received")
        if (::socket.isInitialized) {
            socket.close()
        }
        return@withContext result
    }

    override suspend fun release() {
        withContext(Dispatchers.IO) {
            if (::socket.isInitialized) {
                socket.close()
            }
        }
    }

    fun shortArrayToLittleEndianByteArray(shorts: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shorts.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { byteBuffer.putShort(it) }
        return byteBuffer.array()
    }

    suspend fun readTranscriptionResult(): String {
        return try {
            val (header, data, _) = readWyomingMessage()

            if (header.optString("type") == "transcript") {
                data.optString("text", "")
            } else {
                Log.w("wyoming", "Unerwarteter Nachrichtentyp: ${header.optString("type")}")
                ""
            }
        } catch (e: Exception) {
            Log.e("wyoming", "Fehler beim Lesen der Transkriptionsantwort: ${e.message}")
            ""
        }
    }

    suspend fun readWyomingMessage(): Triple<JSONObject, JSONObject, ByteArray> {
        // 1. Header-Zeile lesen (endet mit \n)
        val headerLine = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte == -1) throw RuntimeException("Verbindung beim Lesen des Headers beendet")
            if (byte.toChar() == '\n') break
            headerLine.append(byte.toChar())
        }

        val headerJson = JSONObject(headerLine.toString())
        val dataLength = headerJson.optInt("data_length", 0)
        val payloadLength = headerJson.optInt("payload_length", 0)

        // 2. Optional: Data lesen
        val dataBytes = if (dataLength > 0) {
            readExactNBytes(input, dataLength)
        } else {
            ByteArray(0)
        }

        val dataJson = if (dataBytes.isNotEmpty()) {
            JSONObject(String(dataBytes, StandardCharsets.UTF_8))
        } else {
            JSONObject()
        }

        // 3. Optional: Payload lesen
        val payload = if (payloadLength > 0) {
            readExactNBytes(input, payloadLength)
        } else {
            ByteArray(0)
        }

        return Triple(headerJson, dataJson, payload)
    }

    fun readExactNBytes(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var bytesRead = 0
        while (bytesRead < length) {
            val read = input.read(buffer, bytesRead, length - bytesRead)
            if (read == -1) throw RuntimeException("Verbindung beim Lesen von $length Bytes beendet")
            bytesRead += read
        }
        return buffer
    }
}
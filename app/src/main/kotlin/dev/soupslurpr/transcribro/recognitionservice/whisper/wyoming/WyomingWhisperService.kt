package dev.soupslurpr.transcribro.recognitionservice.whisper.wyoming

import android.util.Log
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperService
import dev.soupslurpr.transcribro.recognitionservice.whisper.WhisperUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class WyomingWhisperService(
    private val host: String = "localhost",
    private val port: Int = 10300,
    private val ssl: Boolean = true,
) : WhisperService {
    companion object {
        private const val TAG = "wyoming"
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val AVAILABILITY_TIMEOUT_MS = 1_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { probe ->
                probe.connect(InetSocketAddress(host, port), AVAILABILITY_TIMEOUT_MS)
            }
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.w(TAG, "Wyoming backend $host:$port is not reachable: ${exception.message}")
            false
        }
    }

    private fun establishConnection() {
        val current = socket
        if (current != null && !current.isClosed) {
            return
        }

        closeSocket()
        try {
            val address = InetSocketAddress(host, port)
            val newSocket = if (ssl) {
                val rawSocket = Socket()
                rawSocket.connect(address, CONNECT_TIMEOUT_MS)
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                (factory.createSocket(rawSocket, host, port, true) as SSLSocket).apply {
                    enabledProtocols = arrayOf("TLSv1.2")
                }
            } else {
                Socket().apply {
                    connect(address, CONNECT_TIMEOUT_MS)
                }
            }
            newSocket.soTimeout = READ_TIMEOUT_MS
            socket = newSocket
            output = newSocket.getOutputStream()
            input = newSocket.getInputStream()
        } catch (exception: IOException) {
            closeSocket()
            throw WhisperUnavailableException("Could not connect to Wyoming at $host:$port", exception)
        }
    }

    private fun sendEvent(
        header: JSONObject,
        data: JSONObject? = null,
        payload: ByteArray? = null,
    ) {
        val stream = output ?: throw WhisperUnavailableException("Wyoming socket is not connected")
        val headerCopy = JSONObject(header.toString())
        val dataBytes = data?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val payloadBytes = payload ?: ByteArray(0)

        if (data != null) headerCopy.put("data_length", dataBytes.size)
        if (payload != null) headerCopy.put("payload_length", payloadBytes.size)

        try {
            stream.write((headerCopy.toString() + "\n").toByteArray(Charsets.UTF_8))
            if (dataBytes.isNotEmpty()) stream.write(dataBytes)
            if (payloadBytes.isNotEmpty()) stream.write(payloadBytes)
            stream.flush()
        } catch (exception: IOException) {
            closeSocket()
            throw WhisperUnavailableException("Could not send data to Wyoming", exception)
        }

        Log.d(TAG, "Data JSON: ${data?.toString()}")
    }

    override suspend fun startTranscription(lang: String) = withContext(Dispatchers.IO) {
        try {
            establishConnection()

            sendEvent(
                header = JSONObject(mapOf("type" to "transcribe", "version" to "1.7.1")),
                data = JSONObject(mapOf("language" to lang)),
            )

            val audioMeta = JSONObject(
                mapOf(
                    "rate" to 16000,
                    "width" to 2,
                    "channels" to 1,
                    "timestamp" to JSONObject.NULL,
                ),
            )

            sendEvent(
                header = JSONObject(mapOf("type" to "audio-start", "version" to "1.7.1")),
                data = audioMeta,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: WhisperUnavailableException) {
            throw exception
        } catch (exception: Exception) {
            closeSocket()
            throw WhisperUnavailableException("Could not start Wyoming transcription", exception)
        }
    }

    override suspend fun transcribeAudio(data: ShortArray): String =
        withContext(Dispatchers.IO) {
            try {
                establishConnection()

                val chunkSizeSamples = 4000 // 250 ms @ 16 kHz
                var offset = 0
                var timestampMs = 0

                while (offset < data.size) {
                    val end = minOf(offset + chunkSizeSamples, data.size)
                    val chunkSamples = data.sliceArray(offset until end)
                    val payload = shortArrayToLittleEndianByteArray(chunkSamples)

                    val audioMeta = JSONObject(
                        mapOf(
                            "rate" to 16000,
                            "width" to 2,
                            "channels" to 1,
                            "timestamp" to timestampMs,
                        ),
                    )

                    sendEvent(
                        header = JSONObject(mapOf("type" to "audio-chunk", "version" to "1.7.1")),
                        data = audioMeta,
                        payload = payload,
                    )

                    offset = end
                    timestampMs += (chunkSamples.size * 1000) / 16000
                }

                ""
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: WhisperUnavailableException) {
                throw exception
            } catch (exception: Exception) {
                closeSocket()
                throw WhisperUnavailableException("Could not send audio to Wyoming", exception)
            }
        }

    override suspend fun endTranscription(): String = withContext(Dispatchers.IO) {
        try {
            establishConnection()

            sendEvent(
                header = JSONObject(mapOf("type" to "audio-stop", "version" to "1.7.1")),
                data = JSONObject(),
            )

            readTranscriptionResult()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: WhisperUnavailableException) {
            throw exception
        } catch (exception: Exception) {
            throw WhisperUnavailableException("Could not finish Wyoming transcription", exception)
        } finally {
            closeSocket()
        }
    }

    override suspend fun release() {
        withContext(Dispatchers.IO) {
            closeSocket()
        }
    }

    private fun closeSocket() {
        input = null
        output = null
        socket?.let { current ->
            try {
                current.close()
            } catch (exception: IOException) {
                Log.w(TAG, "Could not close Wyoming socket", exception)
            }
        }
        socket = null
    }

    private fun shortArrayToLittleEndianByteArray(shorts: ShortArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(shorts.size * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { byteBuffer.putShort(it) }
        return byteBuffer.array()
    }

    private fun readTranscriptionResult(): String {
        val (header, data, _) = readWyomingMessage()
        if (header.optString("type") != "transcript") {
            throw WhisperUnavailableException(
                "Unexpected Wyoming message type: ${header.optString("type")}",
            )
        }
        return data.optString("text", "")
    }

    private fun readWyomingMessage(): Triple<JSONObject, JSONObject, ByteArray> {
        val stream = input ?: throw WhisperUnavailableException("Wyoming socket is not connected")

        val headerLine = StringBuilder()
        while (true) {
            val byte = stream.read()
            if (byte == -1) {
                throw WhisperUnavailableException("Connection closed while reading Wyoming header")
            }
            if (byte.toChar() == '\n') break
            headerLine.append(byte.toChar())
        }

        val headerJson = JSONObject(headerLine.toString())
        val dataLength = headerJson.optInt("data_length", 0)
        val payloadLength = headerJson.optInt("payload_length", 0)

        val dataBytes = if (dataLength > 0) {
            readExactNBytes(stream, dataLength)
        } else {
            ByteArray(0)
        }

        val dataJson = if (dataBytes.isNotEmpty()) {
            JSONObject(String(dataBytes, StandardCharsets.UTF_8))
        } else {
            JSONObject()
        }

        val payload = if (payloadLength > 0) {
            readExactNBytes(stream, payloadLength)
        } else {
            ByteArray(0)
        }

        return Triple(headerJson, dataJson, payload)
    }

    private fun readExactNBytes(stream: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var bytesRead = 0
        while (bytesRead < length) {
            val read = stream.read(buffer, bytesRead, length - bytesRead)
            if (read == -1) {
                throw WhisperUnavailableException(
                    "Connection closed while reading $length bytes from Wyoming",
                )
            }
            bytesRead += read
        }
        return buffer
    }
}

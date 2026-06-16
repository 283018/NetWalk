package edu.pwr.zpi.netwalk.network

// import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.MeasurementRequest
import edu.pwr.zpi.netwalk.logD
import edu.pwr.zpi.netwalk.logE
import edu.pwr.zpi.netwalk.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NetworkClient(
    private val baseUrl: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    suspend fun checkHealth(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val url = URL("$baseUrl/health")

                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 3000
                }

                val code = connection.responseCode
                val latency = System.currentTimeMillis() - startTime
                logD("[NetworkClient: checkHealth] Response: code=$code, latency=${latency}ms")

                if (code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = Exception("HTTP $code on /health")
                    logE("[NetworkClient: checkHealth] Health check failed: $code")
                    Result.failure(err)
                }
            } catch (e: Exception) {
                logE("[NetworkClient: checkHealth] Exception: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun sendFullUpdate(data: MeasurementRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/measurements/batch")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                }

                val jsonData = json.encodeToString(data)
                logI("[NetworkClient: sendFullUpdate] POSTing payload, size=${jsonData.length} bytes")
                val startTime = System.currentTimeMillis()

                OutputStreamWriter(connection.outputStream).use { it.write(jsonData) }

                val code = connection.responseCode
                val latency = System.currentTimeMillis() - startTime
                logD("[NetworkClient: sendFullUpdate] Response: code=$code, latency=${latency}ms")

                // 200 - 299 odpowiada sukcesowi
                if (code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = Exception("HTTP $code")
                    logE("[NetworkClient: sendFullUpdate] Failed: $code")
                    Result.failure(err)
                }
            } catch (e: Exception) {
                logE("[NetworkClient: sendFullUpdate] Exception: ${e.message}")
                Result.failure(e)
            }
        }

    suspend fun sendGzippedUpdate(gzippedBody: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/measurements/batch")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Content-Encoding", "gzip")
                    connectTimeout = 5000
                }
                logI("[NetworkClient: sendGzippedUpdate] POSTing gzipped payload, size=${gzippedBody.size} bytes")
                val startTime = System.currentTimeMillis()
                connection.outputStream.use { it.write(gzippedBody) }

                val code = connection.responseCode
                val latency = System.currentTimeMillis() - startTime
                logD("[NetworkClient: sendGzippedUpdate] Response: code=$code, latency=${latency}ms")

                if (code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = Exception("HTTP $code")
                    logE("[NetworkClient: sendGzippedUpdate] Failed: $code")
                    Result.failure(err)
                }
            } catch (e: Exception) {
                logE("[NetworkClient: sendGzippedUpdate] Exception: ${e.message}")
                Result.failure(e)
            }
        }
}

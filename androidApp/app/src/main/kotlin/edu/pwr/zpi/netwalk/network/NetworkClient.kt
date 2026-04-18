package edu.pwr.zpi.netwalk.network

import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NetworkClient(
    private val baseUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendFullUpdate(data: NetworkInfoData): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/send")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                }

                val jsonData = json.encodeToString(data)

                OutputStreamWriter(connection.outputStream).use { it.write(jsonData) }

                // 200 - 299 odpowiada sukcesowi
                if (connection.responseCode in 200..299) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${connection.responseCode}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

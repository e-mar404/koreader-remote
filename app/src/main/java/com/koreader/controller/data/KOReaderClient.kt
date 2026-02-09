package com.koreader.controller.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class PageTurnResult {
    object Success : PageTurnResult()
    data class Error(val message: String) : PageTurnResult()
}

sealed class ConnectionStatus {
    object Unknown : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class KOReaderClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "KOReaderHTTP"
        private const val PREVIOUS_PAGE_PATH = "/koreader/event/GotoViewRel/-1"
        private const val NEXT_PAGE_PATH = "/koreader/event/GotoViewRel/1"
    }
    
    private fun buildBaseUrl(ip: String, port: String): String {
        return "http://$ip:$port"
    }
    
    suspend fun turnPage(
        ipAddress: String,
        port: String,
        direction: PageDirection
    ): PageTurnResult = withContext(Dispatchers.IO) {
        if (!isValidIpAddress(ipAddress) || !isValidPort(port)) {
            Log.e(TAG, "Invalid IP address or port: $ipAddress:$port")
            return@withContext PageTurnResult.Error("Invalid IP address or port")
        }

        val path = when (direction) {
            PageDirection.PREVIOUS -> PREVIOUS_PAGE_PATH
            PageDirection.NEXT -> NEXT_PAGE_PATH
        }

        val url = "${buildBaseUrl(ipAddress, port)}$path"
        Log.d(TAG, "Sending page turn request: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Page turn successful: HTTP ${response.code}")
                    PageTurnResult.Success
                } else {
                    Log.e(TAG, "Page turn failed: HTTP ${response.code}: ${response.message}")
                    PageTurnResult.Error("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during page turn: ${e.message}")
            PageTurnResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during page turn: ${e.message}")
            PageTurnResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    suspend fun testConnection(ipAddress: String, port: String): ConnectionStatus =
        withContext(Dispatchers.IO) {
            if (!isValidIpAddress(ipAddress) || !isValidPort(port)) {
                Log.e(TAG, "Invalid IP address or port for connection test: $ipAddress:$port")
                return@withContext ConnectionStatus.Error("Invalid IP address or port")
            }

            // Test with a simple request
            val url = "${buildBaseUrl(ipAddress, port)}/"
            Log.d(TAG, "Testing connection to: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            return@withContext try {
                client.newCall(request).execute().use { response: Response ->
                    if (response.isSuccessful || response.code == 404) {
                        // 404 is OK - server is running but path might not exist
                        Log.d(TAG, "Connection test successful: HTTP ${response.code}")
                        ConnectionStatus.Connected
                    } else {
                        Log.e(TAG, "Connection test failed: HTTP ${response.code}")
                        ConnectionStatus.Error("HTTP ${response.code}")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection test failed - Network error: ${e.message}")
                ConnectionStatus.Error("Cannot connect to server")
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed - Unexpected error: ${e.message}")
                ConnectionStatus.Error("Unexpected error: ${e.message}")
            }
        }
}

enum class PageDirection {
    PREVIOUS,
    NEXT
}

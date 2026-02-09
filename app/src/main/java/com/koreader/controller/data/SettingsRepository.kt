package com.koreader.controller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

sealed class SettingsResult {
    data class Success(val settings: Settings) : SettingsResult()
    data class Error(val exception: Throwable) : SettingsResult()
}

data class Settings(
    val ipAddress: String,
    val port: String
) {
    fun isValid(): Boolean {
        return isValidIpAddress(ipAddress) && isValidPort(port)
    }
}

fun isValidIpAddress(ip: String): Boolean {
    if (ip.isBlank()) return false
    
    // IPv4 validation
    val ipv4Pattern = Regex("""
        ^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}
        (25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$
    """.trimIndent().replace("\n", ""))
    
    return ipv4Pattern.matches(ip)
}

fun isValidPort(port: String): Boolean {
    return try {
        val portNum = port.toInt()
        portNum in 1..65535
    } catch (e: NumberFormatException) {
        false
    }
}

class SettingsRepository(private val context: Context) {
    
    private companion object {
        val IP_ADDRESS_KEY = stringPreferencesKey("ip_address")
        val PORT_KEY = stringPreferencesKey("port")
        const val DEFAULT_IP = "192.168.1.100"
        const val DEFAULT_PORT = "8080"
    }
    
    val settingsFlow: Flow<SettingsResult> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ip = preferences[IP_ADDRESS_KEY] ?: DEFAULT_IP
            val port = preferences[PORT_KEY] ?: DEFAULT_PORT
            SettingsResult.Success(Settings(ip, port))
        }
    
    suspend fun saveSettings(ipAddress: String, port: String): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[IP_ADDRESS_KEY] = ipAddress
                preferences[PORT_KEY] = port
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSettings(): SettingsResult {
        return try {
            val settings = context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { prefs ->
                    val ip = prefs[IP_ADDRESS_KEY] ?: DEFAULT_IP
                    val port = prefs[PORT_KEY] ?: DEFAULT_PORT
                    Settings(ip, port)
                }
                .first()
            SettingsResult.Success(settings)
        } catch (e: Exception) {
            SettingsResult.Error(e)
        }
    }
}

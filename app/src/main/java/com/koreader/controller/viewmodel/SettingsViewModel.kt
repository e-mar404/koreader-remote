package com.koreader.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koreader.controller.data.Settings
import com.koreader.controller.data.SettingsRepository
import com.koreader.controller.data.SettingsResult
import com.koreader.controller.data.isValidIpAddress
import com.koreader.controller.data.isValidPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val ipAddress: String = "",
    val port: String = "",
    val isLoading: Boolean = false,
    val isValid: Boolean = false,
    val ipError: String? = null,
    val portError: String? = null,
    val saveSuccess: Boolean = false,
    val saveError: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            settingsRepository.settingsFlow.collect { result ->
                when (result) {
                    is SettingsResult.Success -> {
                        val settings = result.settings
                        _uiState.value = SettingsUiState(
                            ipAddress = settings.ipAddress,
                            port = settings.port,
                            isLoading = false,
                            isValid = validateInputs(settings.ipAddress, settings.port)
                        )
                    }
                    is SettingsResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            saveError = "Failed to load settings"
                        )
                    }
                }
            }
        }
    }

    fun onIpAddressChanged(ip: String) {
        val validationResult = validateIpAddress(ip)
        _uiState.value = _uiState.value.copy(
            ipAddress = ip,
            ipError = validationResult,
            isValid = validateInputs(ip, _uiState.value.port),
            saveSuccess = false,
            saveError = null
        )
    }

    fun onPortChanged(port: String) {
        val validationResult = validatePort(port)
        _uiState.value = _uiState.value.copy(
            port = port,
            portError = validationResult,
            isValid = validateInputs(_uiState.value.ipAddress, port),
            saveSuccess = false,
            saveError = null
        )
    }

    fun saveSettings() {
        val currentState = _uiState.value

        if (!currentState.isValid) {
            _uiState.value = currentState.copy(
                ipError = validateIpAddress(currentState.ipAddress),
                portError = validatePort(currentState.port)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true)

            val result = settingsRepository.saveSettings(
                currentState.ipAddress,
                currentState.port
            )

            result.fold(
                onSuccess = {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        saveSuccess = true,
                        saveError = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        saveSuccess = false,
                        saveError = "Failed to save: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            saveSuccess = false,
            saveError = null
        )
    }

    private fun validateInputs(ip: String, port: String): Boolean {
        return validateIpAddress(ip) == null && validatePort(port) == null
    }

    private fun validateIpAddress(ip: String): String? {
        return if (ip.isBlank()) {
            "IP address is required"
        } else if (!isValidIpAddress(ip)) {
            "Invalid IPv4 address format (e.g., 192.168.1.100)"
        } else {
            null
        }
    }

    private fun validatePort(port: String): String? {
        return if (port.isBlank()) {
            "Port is required"
        } else if (!isValidPort(port)) {
            "Port must be between 1-65535"
        } else {
            null
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

package com.koreader.controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koreader.controller.data.ConnectionStatus
import com.koreader.controller.data.KOReaderClient
import com.koreader.controller.data.PageDirection
import com.koreader.controller.data.PageTurnResult
import com.koreader.controller.data.Settings
import com.koreader.controller.data.SettingsRepository
import com.koreader.controller.data.SettingsResult
import com.koreader.controller.data.isValidIpAddress
import com.koreader.controller.data.isValidPort
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ControllerUiState {
    object Loading : ControllerUiState()
    data class Ready(
        val settings: Settings,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
        val lastAction: String? = null,
        val isProcessing: Boolean = false
    ) : ControllerUiState()
    data class Error(val message: String) : ControllerUiState()
}

class ControllerViewModel(
    private val settingsRepository: SettingsRepository,
    private val koreaderClient: KOReaderClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<ControllerUiState>(ControllerUiState.Loading)
    val uiState: StateFlow<ControllerUiState> = _uiState.asStateFlow()

    private var lastPageTurnTime = 0L
    private val pageTurnDebounceMs = 300L
    private var connectionCheckJob: Job? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { result ->
                when (result) {
                    is SettingsResult.Success -> {
                        _uiState.value = ControllerUiState.Ready(
                            settings = result.settings
                        )
                        checkConnection()
                    }
                    is SettingsResult.Error -> {
                        _uiState.value = ControllerUiState.Error(
                            "Failed to load settings: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }

    fun checkConnection() {
        connectionCheckJob?.cancel()
        connectionCheckJob = viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ControllerUiState.Ready) {
                val status = koreaderClient.testConnection(
                    currentState.settings.ipAddress,
                    currentState.settings.port
                )
                _uiState.value = currentState.copy(connectionStatus = status)
            }
        }
    }

    fun onDpadLeft(): Boolean {
        return handlePageTurn(PageDirection.PREVIOUS)
    }

    fun onDpadRight(): Boolean {
        return handlePageTurn(PageDirection.NEXT)
    }

    private fun handlePageTurn(direction: PageDirection): Boolean {
        val currentState = _uiState.value
        if (currentState !is ControllerUiState.Ready || currentState.isProcessing) {
            return false
        }

        // Debounce to prevent rapid fire
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPageTurnTime < pageTurnDebounceMs) {
            return true
        }
        lastPageTurnTime = currentTime

        viewModelScope.launch {
            _uiState.value = currentState.copy(isProcessing = true)

            val result = koreaderClient.turnPage(
                currentState.settings.ipAddress,
                currentState.settings.port,
                direction
            )

            val actionText = when (direction) {
                PageDirection.PREVIOUS -> "Previous Page"
                PageDirection.NEXT -> "Next Page"
            }

            val statusText = when (result) {
                is PageTurnResult.Success -> "$actionText - OK"
                is PageTurnResult.Error -> "$actionText - Failed: ${result.message}"
            }

            _uiState.value = currentState.copy(
                isProcessing = false,
                lastAction = statusText
            )

            // Clear the action message after 2 seconds
            delay(2000)
            val updatedState = _uiState.value
            if (updatedState is ControllerUiState.Ready && updatedState.lastAction == statusText) {
                _uiState.value = updatedState.copy(lastAction = null)
            }
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        connectionCheckJob?.cancel()
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val koreaderClient: KOReaderClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ControllerViewModel::class.java)) {
                return ControllerViewModel(settingsRepository, koreaderClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

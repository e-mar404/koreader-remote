package com.koreader.controller.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koreader.controller.data.Command
import com.koreader.controller.data.ConnectionStatus
import com.koreader.controller.data.KOReaderClient
import com.koreader.controller.data.PageDirection
import com.koreader.controller.data.Settings
import com.koreader.controller.data.SettingsRepository
import com.koreader.controller.data.SettingsResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Represents a gamepad button that can be mapped
data class GamepadButton(
    val keyCode: Int,
    val displayName: String,
    val position: ButtonPosition
)

enum class ButtonPosition {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y,
    L1, L2, R1, R2,
    SELECT, START
}

// Available gamepad buttons
val availableButtons = listOf(
    GamepadButton(KeyEvent.KEYCODE_DPAD_UP, "D-Up", ButtonPosition.DPAD_UP),
    GamepadButton(KeyEvent.KEYCODE_DPAD_DOWN, "D-Down", ButtonPosition.DPAD_DOWN),
    GamepadButton(KeyEvent.KEYCODE_DPAD_LEFT, "D-Left", ButtonPosition.DPAD_LEFT),
    GamepadButton(KeyEvent.KEYCODE_DPAD_RIGHT, "D-Right", ButtonPosition.DPAD_RIGHT),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_A, "A", ButtonPosition.BUTTON_A),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_B, "B", ButtonPosition.BUTTON_B),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_X, "X", ButtonPosition.BUTTON_X),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_Y, "Y", ButtonPosition.BUTTON_Y),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_L1, "L1", ButtonPosition.L1),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_L2, "L2", ButtonPosition.L2),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_R1, "R1", ButtonPosition.R1),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_R2, "R2", ButtonPosition.R2),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_SELECT, "-", ButtonPosition.SELECT),
    GamepadButton(KeyEvent.KEYCODE_BUTTON_START, "+", ButtonPosition.START)
)

sealed class ControllerUiState {
    object Loading : ControllerUiState()
    data class Ready(
        val settings: Settings,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Unknown,
        val buttonMappings: Map<Int, Command> = emptyMap(),
        val pressedButton: Int? = null,
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
    private val _buttonMappings = mutableMapOf<Int, Command>()

    init {
        loadSettings()
        // Initialize with default mappings
        _buttonMappings[KeyEvent.KEYCODE_DPAD_LEFT] = Command.PreviousPage
        _buttonMappings[KeyEvent.KEYCODE_DPAD_RIGHT] = Command.NextPage
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { result ->
                when (result) {
                    is SettingsResult.Success -> {
                        _uiState.value = ControllerUiState.Ready(
                            settings = result.settings,
                            buttonMappings = _buttonMappings.toMap()
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

    fun onButtonPressed(keyCode: Int): Boolean {
        val currentState = _uiState.value
        if (currentState !is ControllerUiState.Ready) return false

        // Update UI to show pressed state
        _uiState.value = currentState.copy(pressedButton = keyCode)

        // Execute mapped command if exists
        val command = _buttonMappings[keyCode]
        if (command != null) {
            executeCommand(command)
            return true
        }

        return false
    }

    fun onButtonReleased(keyCode: Int) {
        val currentState = _uiState.value
        if (currentState is ControllerUiState.Ready && currentState.pressedButton == keyCode) {
            _uiState.value = currentState.copy(pressedButton = null)
        }
    }

    fun setButtonMapping(keyCode: Int, command: Command?) {
        if (command != null) {
            _buttonMappings[keyCode] = command
        } else {
            _buttonMappings.remove(keyCode)
        }
        
        val currentState = _uiState.value
        if (currentState is ControllerUiState.Ready) {
            _uiState.value = currentState.copy(buttonMappings = _buttonMappings.toMap())
        }
    }

    private fun executeCommand(command: Command) {
        val currentState = _uiState.value
        if (currentState !is ControllerUiState.Ready || currentState.isProcessing) return

        // Debounce
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPageTurnTime < pageTurnDebounceMs) return
        lastPageTurnTime = currentTime

        viewModelScope.launch {
            _uiState.value = currentState.copy(isProcessing = true)

            val direction = when (command) {
                is Command.PreviousPage -> PageDirection.PREVIOUS
                is Command.NextPage -> PageDirection.NEXT
            }

            val result = koreaderClient.turnPage(
                currentState.settings.ipAddress,
                currentState.settings.port,
                direction
            )

            val statusText = when (result) {
                is com.koreader.controller.data.PageTurnResult.Success -> "${command.displayName} - OK"
                is com.koreader.controller.data.PageTurnResult.Error -> "${command.displayName} - Failed: ${result.message}"
            }

            _uiState.value = currentState.copy(
                isProcessing = false,
                lastAction = statusText
            )

            delay(2000)
            val updatedState = _uiState.value
            if (updatedState is ControllerUiState.Ready && updatedState.lastAction == statusText) {
                _uiState.value = updatedState.copy(lastAction = null)
            }
        }
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

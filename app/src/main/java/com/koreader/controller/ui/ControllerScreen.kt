package com.koreader.controller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koreader.controller.data.Command
import com.koreader.controller.viewmodel.ButtonPosition
import com.koreader.controller.viewmodel.ControllerUiState
import com.koreader.controller.viewmodel.ControllerViewModel
import com.koreader.controller.viewmodel.GamepadButton
import com.koreader.controller.viewmodel.availableButtons

@Composable
fun ControllerScreen(viewModel: ControllerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedButton by remember { mutableStateOf<GamepadButton?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val state = uiState) {
            is ControllerUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is ControllerUiState.Ready -> {
                GamepadLayout(
                    state = state,
                    onConfigureClick = { selectedButton = it }
                )
            }
            is ControllerUiState.Error -> {
                ErrorContent(state.message)
            }
        }

        // Command selection dialog
        selectedButton?.let { button ->
            val state = uiState as? ControllerUiState.Ready
            val currentCommand = state?.buttonMappings?.get(button.keyCode)

            CommandSelectionDialog(
                button = button,
                currentCommand = currentCommand,
                onDismiss = { selectedButton = null },
                onCommandSelected = { command ->
                    viewModel.setButtonMapping(button.keyCode, command)
                    selectedButton = null
                }
            )
        }
    }
}

@Composable
private fun GamepadLayout(
    state: ControllerUiState.Ready,
    onConfigureClick: (GamepadButton) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Gamepad",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Last action display
            state.lastAction?.let { action ->
                Text(
                    text = action,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (action.contains("OK")) Color.Green else Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Shoulder buttons (L1, L2, R1, R2)
            ShoulderButtonsRow(
                pressedButton = state.pressedButton,
                onConfigureClick = onConfigureClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Select/Start buttons (-/+) - now between shoulder buttons and main controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectStartButtons(
                    pressedButton = state.pressedButton,
                    onConfigureClick = onConfigureClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main controls - D-Pad and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-Pad (Left side)
                DpadLayout(
                    pressedButton = state.pressedButton,
                    onConfigureClick = onConfigureClick
                )

                // Action buttons (Right side)
                ActionButtonsLayout(
                    pressedButton = state.pressedButton,
                    onConfigureClick = onConfigureClick
                )
            }
        }
    }
}

@Composable
private fun ShoulderButtonsRow(
    pressedButton: Int?,
    onConfigureClick: (GamepadButton) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left shoulder buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val l1Button = availableButtons.find { it.position == ButtonPosition.L1 }!!
            val l2Button = availableButtons.find { it.position == ButtonPosition.L2 }!!

            GamepadButtonView(
                button = l2Button,
                isPressed = pressedButton == l2Button.keyCode,
                onClick = { onConfigureClick(l2Button) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            GamepadButtonView(
                button = l1Button,
                isPressed = pressedButton == l1Button.keyCode,
                onClick = { onConfigureClick(l1Button) }
            )
        }

        // Right shoulder buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val r1Button = availableButtons.find { it.position == ButtonPosition.R1 }!!
            val r2Button = availableButtons.find { it.position == ButtonPosition.R2 }!!

            GamepadButtonView(
                button = r2Button,
                isPressed = pressedButton == r2Button.keyCode,
                onClick = { onConfigureClick(r2Button) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            GamepadButtonView(
                button = r1Button,
                isPressed = pressedButton == r1Button.keyCode,
                onClick = { onConfigureClick(r1Button) }
            )
        }
    }
}

@Composable
private fun DpadLayout(
    pressedButton: Int?,
    onConfigureClick: (GamepadButton) -> Unit
) {
    val upButton = availableButtons.find { it.position == ButtonPosition.DPAD_UP }!!
    val downButton = availableButtons.find { it.position == ButtonPosition.DPAD_DOWN }!!
    val leftButton = availableButtons.find { it.position == ButtonPosition.DPAD_LEFT }!!
    val rightButton = availableButtons.find { it.position == ButtonPosition.DPAD_RIGHT }!!

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Up
        DpadButton(
            button = upButton,
            isPressed = pressedButton == upButton.keyCode,
            onClick = { onConfigureClick(upButton) }
        )

        Row {
            // Left
            DpadButton(
                button = leftButton,
                isPressed = pressedButton == leftButton.keyCode,
                onClick = { onConfigureClick(leftButton) }
            )

            // Center (not a real button, just spacing)
            Box(modifier = Modifier.size(48.dp))

            // Right
            DpadButton(
                button = rightButton,
                isPressed = pressedButton == rightButton.keyCode,
                onClick = { onConfigureClick(rightButton) }
            )
        }

        // Down
        DpadButton(
            button = downButton,
            isPressed = pressedButton == downButton.keyCode,
            onClick = { onConfigureClick(downButton) }
        )
    }
}

@Composable
private fun DpadButton(
    button: GamepadButton,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (button.position) {
                ButtonPosition.DPAD_UP -> "▲"
                ButtonPosition.DPAD_DOWN -> "▼"
                ButtonPosition.DPAD_LEFT -> "◀"
                ButtonPosition.DPAD_RIGHT -> "▶"
                else -> ""
            },
            fontSize = 20.sp,
            color = if (isPressed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionButtonsLayout(
    pressedButton: Int?,
    onConfigureClick: (GamepadButton) -> Unit
) {
    val aButton = availableButtons.find { it.position == ButtonPosition.BUTTON_A }!!
    val bButton = availableButtons.find { it.position == ButtonPosition.BUTTON_B }!!
    val xButton = availableButtons.find { it.position == ButtonPosition.BUTTON_X }!!
    val yButton = availableButtons.find { it.position == ButtonPosition.BUTTON_Y }!!

    // Swapped layout: X on top, Y left, A right, B bottom
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // X (now on top)
        ActionButton(
            button = xButton,
            isPressed = pressedButton == xButton.keyCode,
            color = Color(0xFF448AFF), // Blue
            onClick = { onConfigureClick(xButton) }
        )

        Row {
            // Y (now on left)
            ActionButton(
                button = yButton,
                isPressed = pressedButton == yButton.keyCode,
                color = Color(0xFFFF5252), // Red
                onClick = { onConfigureClick(yButton) }
            )

            Spacer(modifier = Modifier.size(48.dp))

            // A (now on right)
            ActionButton(
                button = aButton,
                isPressed = pressedButton == aButton.keyCode,
                color = Color(0xFF69F0AE), // Green
                onClick = { onConfigureClick(aButton) }
            )
        }

        // B (now on bottom)
        ActionButton(
            button = bButton,
            isPressed = pressedButton == bButton.keyCode,
            color = Color(0xFFFFAB40), // Orange
            onClick = { onConfigureClick(bButton) }
        )
    }
}

@Composable
private fun ActionButton(
    button: GamepadButton,
    isPressed: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPressed) {
        color.copy(alpha = 1f)
    } else {
        color.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.displayName,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SelectStartButtons(
    pressedButton: Int?,
    onConfigureClick: (GamepadButton) -> Unit
) {
    val selectButton = availableButtons.find { it.position == ButtonPosition.SELECT }!!
    val startButton = availableButtons.find { it.position == ButtonPosition.START }!!

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GamepadButtonView(
            button = selectButton,
            isPressed = pressedButton == selectButton.keyCode,
            onClick = { onConfigureClick(selectButton) }
        )

        GamepadButtonView(
            button = startButton,
            isPressed = pressedButton == startButton.keyCode,
            onClick = { onConfigureClick(startButton) }
        )
    }
}

@Composable
private fun GamepadButtonView(
    button: GamepadButton,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isPressed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.displayName,
            fontSize = 12.sp,
            color = if (isPressed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommandSelectionDialog(
    button: GamepadButton,
    currentCommand: Command?,
    onDismiss: () -> Unit,
    onCommandSelected: (Command?) -> Unit
) {
    var selectedCommand by remember { mutableStateOf<Command?>(currentCommand) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${button.displayName}") },
        text = {
            Column {
                Text(
                    text = "Select a command for this button:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // "None" option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedCommand = null }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = selectedCommand == null,
                        onClick = { selectedCommand = null }
                    )
                    Text(
                        text = "None (Unmapped)",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Available commands
                Command.allCommands.forEach { command ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCommand = command }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedCommand == command,
                            onClick = { selectedCommand = command }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = command.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = command.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCommandSelected(selectedCommand) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = Color.Red,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

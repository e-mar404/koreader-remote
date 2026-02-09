package com.koreader.controller.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
 
import com.koreader.controller.R
import com.koreader.controller.data.ConnectionStatus
import com.koreader.controller.ui.theme.GreenSuccess
import com.koreader.controller.ui.theme.OrangeWarning
import com.koreader.controller.ui.theme.RedError
import com.koreader.controller.viewmodel.ControllerUiState
import com.koreader.controller.viewmodel.ControllerViewModel
import com.koreader.controller.viewmodel.SettingsViewModel
 

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    controllerViewModel: ControllerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val controllerState by controllerViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by remember { mutableStateOf(false) }

    // Reverted: remove gamepad/back-like input swallowing here to revert to original behavior

    // Handle success/error messages
    LaunchedEffect(uiState.saveSuccess, uiState.saveError) {
        when {
            uiState.saveSuccess -> {
                snackbarHostState.showSnackbar("Settings saved successfully")
                viewModel.clearMessages()
                isEditing = false
            }
            uiState.saveError != null -> {
                snackbarHostState.showSnackbar(uiState.saveError!!)
                viewModel.clearMessages()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.title_settings),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Unified Banner - Connection Status with Edit functionality
            if (controllerState is ControllerUiState.Ready) {
                val state = controllerState as ControllerUiState.Ready
                UnifiedConnectionBanner(
                    status = state.connectionStatus,
                    settings = state.settings,
                    ipAddress = uiState.ipAddress,
                    port = uiState.port,
                    ipError = uiState.ipError,
                    portError = uiState.portError,
                    isValid = uiState.isValid,
                    isLoading = uiState.isLoading,
                    isEditing = isEditing,
                    isRefreshing = state.isProcessing,
                    onRefresh = { controllerViewModel.checkConnection() },
                    onEditClick = { isEditing = !isEditing },
                    onIpChange = viewModel::onIpAddressChanged,
                    onPortChange = viewModel::onPortChanged,
                    onSave = viewModel::saveSettings
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = if (uiState.saveSuccess) GreenSuccess else RedError
            )
        }
    }
}

@Composable
private fun UnifiedConnectionBanner(
    status: ConnectionStatus,
    settings: com.koreader.controller.data.Settings,
    ipAddress: String,
    port: String,
    ipError: String?,
    portError: String?,
    isValid: Boolean,
    isLoading: Boolean,
    isEditing: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onEditClick: () -> Unit,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val (statusIcon, statusColor, statusText) = when (status) {
        is ConnectionStatus.Connected -> Triple(
            Icons.Default.CheckCircle,
            GreenSuccess,
            "Connected"
        )
        is ConnectionStatus.Error -> Triple(
            Icons.Default.Error,
            RedError,
            "Connection Error"
        )
        is ConnectionStatus.Unknown -> Triple(
            Icons.Default.Refresh,
            OrangeWarning,
            "Disconnected"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with status only (edit button moved to the row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator only
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current settings display (always visible)
            if (!isEditing) {
                // View mode - show current connection info in a single row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // IP:Port on the left
                        Text(
                            text = "$ipAddress:$port",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Refresh and Edit buttons on the right
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onRefresh,
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = onEditClick) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                // Edit mode - show input fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Edit Connection Settings",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // IP Address Field
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = onIpChange,
                            label = { Text("IP Address") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = ipError != null,
                            supportingText = {
                                ipError?.let {
                                    Text(text = it, color = RedError)
                                }
                            },
                            trailingIcon = {
                                ipError?.let {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = RedError
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        // Port Field
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = portError != null,
                            supportingText = {
                                portError?.let {
                                    Text(text = it, color = RedError)
                                }
                            },
                            trailingIcon = {
                                portError?.let {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = RedError
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEditClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = onSave,
                                modifier = Modifier.weight(1f),
                                enabled = isValid && !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

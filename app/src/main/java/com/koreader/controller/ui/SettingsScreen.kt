package com.koreader.controller.ui

import android.content.Context
import android.os.PowerManager
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
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
    settingsViewModel: SettingsViewModel,
    controllerViewModel: ControllerViewModel,
    isUltraDimMode: Boolean = false
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val controllerState by controllerViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess, uiState.saveError) {
        when {
            uiState.saveSuccess -> {
                snackbarHostState.showSnackbar("Settings saved successfully")
                settingsViewModel.clearMessages()
                isEditing = false
            }
            uiState.saveError != null -> {
                snackbarHostState.showSnackbar(uiState.saveError!!)
                settingsViewModel.clearMessages()
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

            if (controllerState is ControllerUiState.Ready) {
                val state = controllerState as ControllerUiState.Ready
                ConnectionSettingsCard(
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
                    onIpChange = settingsViewModel::onIpAddressChanged,
                    onPortChange = settingsViewModel::onPortChanged,
                    onSave = settingsViewModel::saveSettings
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            BatteryOptimizationCard()
            
            if (isUltraDimMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ultra Dim Mode is ON",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Screen is dimmed to 8% brightness (92% dark overlay). Controller remains active. Tap anywhere to restore normal brightness.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

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
private fun ConnectionSettingsCard(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            if (!isEditing) {
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
                        Text(
                            text = "$ipAddress:$port",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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

@Composable
private fun BatteryOptimizationCard() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIgnoringBatteryOptimizations)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = if (isIgnoringBatteryOptimizations) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Battery Optimization",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isIgnoringBatteryOptimizations) {
                Text(
                    text = "Battery optimization is disabled. The app can keep the screen active reliably.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Battery optimization is enabled. This may interfere with the Keep Screen Active feature.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

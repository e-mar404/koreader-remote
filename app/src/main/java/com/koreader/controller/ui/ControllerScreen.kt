package com.koreader.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koreader.controller.R
import com.koreader.controller.data.ConnectionStatus
import com.koreader.controller.ui.theme.GreenSuccess
import com.koreader.controller.ui.theme.OrangeWarning
import com.koreader.controller.ui.theme.RedError
import com.koreader.controller.viewmodel.ControllerUiState
import com.koreader.controller.viewmodel.ControllerViewModel

@Composable
fun ControllerScreen(viewModel: ControllerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
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
                ControllerContent(state)
            }
            is ControllerUiState.Error -> {
                ErrorContent(state.message)
            }
        }
    }
}

@Composable
private fun ControllerContent(state: ControllerUiState.Ready) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Controller Icon
        Icon(
            imageVector = Icons.Default.Gamepad,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.title_controller),
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Connection Status Card
        ConnectionStatusCard(state.connectionStatus, state.settings)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instructions
        Card(
            modifier = Modifier.padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = stringResource(R.string.controller_instructions),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        // Last Action
        state.lastAction?.let { action ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (action.contains("OK")) 
                        GreenSuccess.copy(alpha = 0.1f) 
                    else 
                        RedError.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = action,
                    modifier = Modifier.padding(12.dp),
                    color = if (action.contains("OK")) GreenSuccess else RedError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        if (state.isProcessing) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    status: ConnectionStatus,
    settings: com.koreader.controller.data.Settings
) {
    val (icon, color, text) = when (status) {
        is ConnectionStatus.Connected -> Triple(
            Icons.Default.CheckCircle,
            GreenSuccess,
            stringResource(R.string.connection_status_connected)
        )
        is ConnectionStatus.Error -> Triple(
            Icons.Default.Error,
            RedError,
            stringResource(R.string.connection_status_error)
        )
        is ConnectionStatus.Unknown -> Triple(
            Icons.Default.HelpOutline,
            OrangeWarning,
            stringResource(R.string.connection_status_disconnected)
        )
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${settings.ipAddress}:${settings.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = RedError,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = RedError,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

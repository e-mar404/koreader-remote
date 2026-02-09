package com.koreader.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.koreader.controller.R
import com.koreader.controller.ui.theme.GreenSuccess
import com.koreader.controller.ui.theme.RedError
import com.koreader.controller.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle success/error messages
    LaunchedEffect(uiState.saveSuccess, uiState.saveError) {
        when {
            uiState.saveSuccess -> {
                snackbarHostState.showSnackbar("Settings saved successfully")
                viewModel.clearMessages()
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
            
            // IP Address Field
            OutlinedTextField(
                value = uiState.ipAddress,
                onValueChange = viewModel::onIpAddressChanged,
                label = { Text(stringResource(R.string.settings_ip_hint)) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.ipError != null,
                supportingText = {
                    uiState.ipError?.let {
                        Text(
                            text = it,
                            color = RedError
                        )
                    }
                },
                trailingIcon = {
                    uiState.ipError?.let {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = RedError
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                enabled = !uiState.isLoading
            )
            
            // Port Field
            OutlinedTextField(
                value = uiState.port,
                onValueChange = viewModel::onPortChanged,
                label = { Text(stringResource(R.string.settings_port_hint)) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.portError != null,
                supportingText = {
                    uiState.portError?.let {
                        Text(
                            text = it,
                            color = RedError
                        )
                    }
                },
                trailingIcon = {
                    uiState.portError?.let {
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
                enabled = !uiState.isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.isValid && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(stringResource(R.string.settings_save))
            }
            
            // Validation Status
            if (uiState.isValid && !uiState.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenSuccess,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Configuration valid",
                    color = GreenSuccess,
                    style = MaterialTheme.typography.bodyMedium
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

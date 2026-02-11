package com.koreader.controller

import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.koreader.controller.ui.ControllerScreen
import com.koreader.controller.ui.SettingsScreen
import com.koreader.controller.ui.theme.KOReaderControllerTheme
import com.koreader.controller.viewmodel.ControllerViewModel
import com.koreader.controller.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val label: @Composable () -> Unit) {
    data object Controller : Screen(
        route = "controller",
        icon = { Icon(Icons.Default.Gamepad, contentDescription = null) },
        label = { Text(stringResource(R.string.title_controller)) }
    )
    data object Settings : Screen(
        route = "settings",
        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        label = { Text(stringResource(R.string.title_settings)) }
    )
}

class MainActivity : ComponentActivity() {
    
    private val controllerViewModel: ControllerViewModel by viewModels {
        val app = application as KOReaderControllerApp
        ControllerViewModel.Factory(app.settingsRepository, app.koreaderClient)
    }
    
    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as KOReaderControllerApp
        SettingsViewModel.Factory(app.settingsRepository)
    }
    
    private lateinit var wakeLock: PowerManager.WakeLock
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "KOReaderController::WakeLock"
        )
        wakeLock.acquire(24*60*60*1000L) // 24 hours
        
        // Keep screen on - this is the key to making controller work when "screen is off"
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            KOReaderControllerTheme {
                MainScreen(
                    controllerViewModel = controllerViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Release wake lock when app is destroyed
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    
    companion object {
        private const val TAG = "ControllerInput"
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode ?: return super.dispatchKeyEvent(event)
        
        // Only handle gamepad/controller buttons
        if (!isGamepadButton(keyCode)) {
            return super.dispatchKeyEvent(event)
        }
        
        val keyName = KeyEvent.keyCodeToString(keyCode)
        val deviceName = event.device?.name ?: "Unknown"
        
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                Log.d(TAG, "Button pressed: $keyName (code: $keyCode) from device: $deviceName")
                val consumed = controllerViewModel.onButtonPressed(keyCode)
                if (consumed) {
                    Log.d(TAG, "Button $keyName consumed by app")
                    return true
                }
            }
            KeyEvent.ACTION_UP -> {
                Log.d(TAG, "Button released: $keyName (code: $keyCode)")
                controllerViewModel.onButtonReleased(keyCode)
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
    
    private fun isGamepadButton(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_START -> true
            else -> false
        }
    }
    
    override fun onResume() {
        super.onResume()
        controllerViewModel.checkConnection()
    }
}

@Composable
fun MainScreen(
    controllerViewModel: ControllerViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Controller, Screen.Settings)
    val systemUiController = rememberSystemUiController()
    
    // State for ultra dim mode (saves battery while keeping app active)
    var isUltraDimMode by rememberSaveable { mutableStateOf(false) }
    
    // Get theme colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    
    // Apply system UI changes when dim mode changes
    SideEffect {
        if (isUltraDimMode) {
            // Dim the status bar and navigation bar to minimum
            systemUiController.setStatusBarColor(
                color = Color.Black,
                darkIcons = false
            )
            systemUiController.setNavigationBarColor(
                color = Color.Black,
                darkIcons = false
            )
        } else {
            // Match system bars to app theme - use background color for seamless look
            systemUiController.setStatusBarColor(
                color = backgroundColor,
                darkIcons = false
            )
            systemUiController.setNavigationBarColor(
                color = backgroundColor,
                darkIcons = false
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Normal UI
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
                            label = screen.label,
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Controller.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Controller.route) {
                    ControllerScreen(
                        viewModel = controllerViewModel,
                        isUltraDimMode = isUltraDimMode,
                        onToggleUltraDimMode = { isUltraDimMode = !isUltraDimMode }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        settingsViewModel = settingsViewModel,
                        controllerViewModel = controllerViewModel,
                        isUltraDimMode = isUltraDimMode
                    )
                }
            }
        }
        
        // Ultra dim overlay - very dark but not completely black (saves battery)
        // Using 92% darkness (0.92f alpha) - visible enough to know it's on, dark enough to save battery
        if (isUltraDimMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { 
                        // Tap anywhere to exit dim mode
                        isUltraDimMode = false 
                    }
            ) {
                // Show dimmed hint text
                Text(
                    text = "Tap to wake",
                    color = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

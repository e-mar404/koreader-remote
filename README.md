# KOReader Controller

Android app to control KOReader via HTTP API using a Bluetooth controller (8BitDo Micro).

## Features

- **Settings Tab**: Configure KOReader IP address and port
- **Controller Tab**: Display connection status and last action
- **Bluetooth Input**: D-Pad Left/Right buttons turn pages
- **Secure Storage**: Settings persisted with Jetpack DataStore
- **HTTP Communication**: OkHttp client with proper error handling
- **Input Debouncing**: Prevents rapid-fire page turns (300ms delay)

## Architecture

- **MVVM Pattern**: ViewModels with Compose UI
- **Repository Pattern**: SettingsRepository for data persistence
- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Latest Material You theming

## Project Structure

```
app/src/main/java/com/koreader/controller/
├── data/
│   ├── SettingsRepository.kt    # DataStore for settings
│   └── KOReaderClient.kt        # HTTP client for KOReader API
├── viewmodel/
│   ├── ControllerViewModel.kt   # Controller screen logic
│   └── SettingsViewModel.kt     # Settings screen logic
├── ui/
│   ├── ControllerScreen.kt      # Controller UI
│   ├── SettingsScreen.kt        # Settings UI
│   └── theme/                   # Material theme
├── MainActivity.kt              # Main activity with bottom nav
└── KOReaderControllerApp.kt     # Application class
```

## KOReader API Endpoints

- **Previous Page**: `GET /koreader/event/GoToViewRel/-1`
- **Next Page**: `GET /koreader/event/GoToViewRel/1`

## Setup

1. Enable KOReader HTTP server:
   - Go to Settings → Network → HTTP Server
   - Enable it and note the IP/port

2. Configure the app:
   - Open Settings tab
   - Enter KOReader IP and port
   - Save

3. Pair your controller:
   - Go to Android Bluetooth settings
   - Pair 8BitDo Micro

4. Use the controller:
   - D-Pad Left: Previous page
   - D-Pad Right: Next page

## Building

### With Android Studio
1. Open project in Android Studio
2. Sync Gradle
3. Run on device or emulator

### With Command Line
```bash
./gradlew assembleDebug
```

## Security

- No hardcoded credentials
- Input validation for IP and port
- Secure settings storage (no cloud backup)
- ProGuard enabled for release builds
- No cleartext traffic restrictions (required for KOReader HTTP)

## Requirements

- Android SDK 26+ (Android 8.0)
- Bluetooth support (for controller)
- KOReader with HTTP server enabled

## Future Enhancements

- [ ] Button remapping
- [ ] Additional KOReader commands
- [ ] Custom themes
- [ ] Widget support

# FindUrDrugz Android App

Native Android client for FindUrDrugz, built with Kotlin and Jetpack Compose. Consumes the REST API in `../../backend`.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Networking:** Retrofit + OkHttp + Moshi
- **Local storage:** Jetpack DataStore (JWT persistence)
- **Navigation:** Navigation Compose
- **State management:** ViewModel + StateFlow
- **Location:** Google Play Services (FusedLocationProviderClient)

## Prerequisites

- Android Studio (current stable release)
- JDK 17 (bundled with recent Android Studio versions)
- The backend running locally — see `../../README.md` and `../../backend/README.md` for setup (Docker, Postgres, `npm run dev`)

## Project Structure

```text
app/src/main/java/com/findurdrugz/android/
├── data/
│   ├── model/          # Kotlin data classes matching backend JSON shapes
│   ├── local/           # TokenManager (DataStore), LocationProvider
│   ├── api/              # Retrofit interface, OkHttp client, auth interceptor
│   └── repository/     # Wraps API calls, converts responses to Result<T>
├── ui/
│   ├── auth/             # Login, Register screens + ViewModel
│   ├── search/         # Medicine search screen + ViewModel
│   ├── order/           # Order/reservation placement screen + ViewModel
│   ├── history/         # Order/reservation history screen + ViewModel
│   ├── home/            # Home screen
│   ├── navigation/    # Screen route definitions
│   └── theme/           # Compose theming
└── MainActivity.kt      # Single-activity app; wires DI + NavHost
```

## Setup

1. Open `frontend/android` as a project in Android Studio (not the repo root — open this subfolder directly).
2. Let Gradle sync. Dependencies are managed via `gradle/libs.versions.toml` plus a few direct `implementation(...)` entries in `app/build.gradle.kts` for Retrofit/Moshi/OkHttp/DataStore/Navigation/Location.
3. Ensure the backend is running locally (`docker-compose up -d` from repo root, then `npm run dev` from `backend/`).

## Running against a local backend (important — networking setup)

The app's base URL is set in `data/api/ApiClient.kt`:
```kotlin
private const val BASE_URL = "http://localhost:3000/"
```

**If you're developing on WSL2** (backend running inside WSL, Android Studio/emulator running on Windows), the emulator cannot reach `localhost` or `10.0.2.2` directly due to WSL2's network layer sitting between Windows and Linux. The fix that works reliably:

```powershell
# Run this in Android Studio's Terminal tab (or any Windows terminal) any time
# the emulator is (re)started — this mapping does not persist across restarts.
adb reverse tcp:3000 tcp:3000
```

If `adb` isn't on your PATH, use the full path:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
```

This forwards the emulator's `localhost:3000` to the backend running in WSL. Without this, you'll see:
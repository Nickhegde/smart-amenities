# SmartAmenities

An Android app that helps passengers locate and navigate to amenities (restrooms, family restrooms, lactation rooms, water fountains) inside DFW Airport terminals. Currently supports **Terminal D** with an interactive floor-plan map, turn-by-turn accessibility-aware navigation, and local user accounts.

---

## Requirements

| Tool | Minimum version |
|------|----------------|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 (Temurin / Eclipse Adoptium recommended) |
| Android SDK | API 35 (compile), API 29+ device/emulator |
| Kotlin | 1.9+ (managed by Gradle) |

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Nickhegde/smart-amenities.git
cd SmartAmenities
```

### 2. Open in Android Studio

- **File → Open** and select the `SmartAmenities` folder (the one containing `settings.gradle.kts`).
- Android Studio will detect the Gradle project and sync automatically.

### 3. Set up `local.properties`

This file is **not** committed (it contains your machine's SDK path). Android Studio normally creates it automatically on first sync. If it is missing, create it at the project root:

```properties
sdk.dir=/path/to/your/Android/sdk
```

Typical paths:
- **macOS**: `sdk.dir=/Users/<you>/Library/Android/sdk`
- **Windows**: `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`
- **Linux**: `sdk.dir=/home/<you>/Android/Sdk`

### 4. Sync & build

Android Studio will prompt you to sync Gradle after opening. You can also sync manually:

> **File → Sync Project with Gradle Files**

Or from the terminal:

```bash
./gradlew assembleDebug
```

---

## Running the App

### On a physical device

1. Enable **Developer Options** on your Android device (Settings → About Phone → tap Build Number 7 times).
2. Enable **USB Debugging** inside Developer Options.
3. Connect via USB and trust the computer prompt on the device.
4. Press the **Run** button in Android Studio (or `Shift+F10`).

### On an emulator

1. Open **Device Manager** in Android Studio (View → Tool Windows → Device Manager).
2. Create a new virtual device — recommend **Pixel 6** with **API 34** image.
3. Start the emulator, then press **Run**.

### From the terminal

```bash
./gradlew installDebug       # builds and installs to a connected device/emulator
```

---

## Build Commands

```bash
./gradlew assembleDebug      # build debug APK  →  app/build/outputs/apk/debug/
./gradlew assembleRelease    # build release APK
./gradlew installDebug       # build + install on connected device/emulator
./gradlew lint               # run Android Lint
./gradlew clean              # delete all build artifacts
./gradlew build              # full build (compile + lint)
```

---

## Screen Flow

```
AuthScreen  (first launch / logged out)
  ├─ Create Account → SignUpScreen ──┐
  ├─ Sign In        → LoginScreen   ─┤ auth success
  └─ Continue as Guest ──────────────┘
                                     ↓
HomeScreen  (terminal selection — Terminal D active)
  └─ Select Terminal D
                                     ↓
MapScreen  (Map tab + List tab)
  └─ Tap pin or list item
                                     ↓
AmenityDetailScreen
  └─ Tap Navigate
                                     ↓
NavigationScreen  (turn-by-turn)
  └─ End / Done  →  back to MapScreen
```

A **Sign Out** / **Sign In** button is present in the top-right corner of every screen for quick account access.

---

## Project Structure

```
SmartAmenities/
├── app/src/main/java/com/smartamenities/
│   ├── data/
│   │   ├── model/          # Domain data classes (Amenity, Route, User, UserPreferences, …)
│   │   ├── local/          # MockAmenityDataSource, UserDataStore (SharedPreferences)
│   │   └── repository/     # AmenityRepository interface + MockAmenityRepository
│   ├── viewmodel/
│   │   ├── AmenityViewModel.kt   # list, filtering, sorting, preferences
│   │   ├── NavigationViewModel.kt# step progress, rerouting
│   │   └── AuthViewModel.kt      # sign-up, login, guest auth, session restore
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── AuthScreen.kt           # landing (create account / sign in / guest)
│   │   │   ├── LoginScreen.kt
│   │   │   ├── SignUpScreen.kt         # name, email, phone, password, accessibility prefs
│   │   │   ├── HomeScreen.kt           # terminal selector
│   │   │   ├── MapScreen.kt            # Canvas floor plan + List tab (Terminal D)
│   │   │   ├── AmenityDetailScreen.kt
│   │   │   ├── NavigationScreen.kt     # turn-by-turn
│   │   │   └── PreferencesScreen.kt    # settings & account
│   │   ├── components/
│   │   │   ├── AccountIconButton.kt    # sign-in/sign-out header button
│   │   │   └── SharedComponents.kt     # cards, chips, badges
│   │   └── theme/                      # Material3 theming (DFW blue/orange palette)
│   ├── navigation/         # Screen sealed class with typed route builders
│   ├── di/                 # Hilt AppModule (swap mock → real backend here)
│   └── MainActivity.kt     # single activity, NavHost
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties        # ← machine-specific, not in git
```

---

## Architecture

**MVVM + Repository** — single-activity with Jetpack Compose Navigation.

- **Data layer**: `MockAmenityDataSource` returns hardcoded Terminal D amenities with 150 ms simulated latency. `UserDataStore` persists user accounts and sessions in `SharedPreferences` (JSON via Gson, passwords SHA-256 hashed). To wire a real backend, replace the `MockAmenityRepository` binding in `di/AppModule.kt` — no other files need changing.
- **ViewModel layer**: UI state is modelled as sealed classes (`Loading / Success / Empty / Error`) exposed via `StateFlow`. `AuthViewModel` restores sessions synchronously on startup.
- **UI layer**: Pure Compose with Material3. `MapScreen` uses a Canvas-based floor plan with multi-floor support (Level 1 Arrivals, Level 3 Gates, Level 4 Mezzanine). Auth back-stack is fully isolated — pressing Back from Home exits the app rather than returning to login screens.

---

## User Data Storage

All user data is stored **locally on the device** — no data is sent to any server.

| What | Where |
|------|-------|
| Registered accounts (name, email, hashed password, phone, accessibility prefs) | `SharedPreferences` — `smartamenities_prefs.xml` |
| Active session (current user JSON) | Same `SharedPreferences` file |
| File location on device | `/data/data/com.smartamenities/shared_prefs/smartamenities_prefs.xml` |

Passwords are stored as **SHA-256 hashes** — the plaintext is never saved.

---

## Key Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose + Material3 | UI |
| Compose Navigation | Single-activity navigation |
| Dagger Hilt | Dependency injection |
| Gson | JSON serialisation for local user storage |
| Room | Local SQLite (planned for Iteration 2) |
| Retrofit + OkHttp | Future backend calls (mocked for now) |
| KSP | Annotation processing for Hilt & Room |

---

## Current Status (Iteration 1)

- All amenity data is mocked — no real backend or database reads yet.
- User accounts (sign-up, login, guest) are fully functional with local SharedPreferences storage.
- Session is persisted across app restarts; the app opens directly to the terminal selector if a session exists.
- Only **Terminal D** is active on the home screen; other terminals show "Coming Soon".
- Room database schema is defined but not yet used for preferences persistence (planned Iteration 2).

---

## Troubleshooting

**Gradle sync fails with "SDK location not found"**
→ Create/fix `local.properties` as described in Step 3 above.

**Build fails with KSP / Hilt errors after a clean**
→ Run `./gradlew clean` then `./gradlew assembleDebug` again. KSP sometimes needs a clean build after changing annotated classes.

**Device not detected**
→ Make sure USB Debugging is on, the USB cable supports data transfer, and you have accepted the "Trust this computer" prompt on the device.

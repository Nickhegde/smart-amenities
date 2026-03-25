# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SmartAmenities is an Android native app (Kotlin + Jetpack Compose) that helps passengers locate and navigate to amenities (restrooms, family restrooms, lactation rooms, water fountains) in Terminal D at DFW Airport. It provides turn-by-turn navigation with accessibility-aware routing and real-time amenity status.

- **Min SDK**: 29 (Android 10), **Target SDK**: 35
- **Application ID**: `com.smartamenities`

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Install to connected device/emulator
./gradlew lint                 # Run Android Lint
./gradlew clean                # Remove build artifacts
./gradlew build                # Full build (compile + lint + test)
```

No test files exist yet (`src/test/` and `src/androidTest/` are not set up).

## Architecture

**Pattern**: MVVM + Repository, single-activity with Jetpack Compose Navigation.

### Layer Structure

- **`data/model/`** — Domain data classes (`Amenity`, `Route`, `NavigationStep`, `UserPreferences`, etc.) defined in `Models.kt`
- **`data/local/`** — `MockAmenityDataSource` with hardcoded Terminal D floor plan data and 150ms simulated latency
- **`data/repository/`** — `AmenityRepository` interface + `MockAmenityRepository` implementation (swap backend by changing one binding in `di/AppModule.kt`)
- **`viewmodel/`** — `AmenityViewModel` (list, filtering, sorting, preferences) and `NavigationViewModel` (step progress, rerouting); UI state via sealed classes (`Loading/Success/Empty/Error`) and `StateFlow`
- **`ui/screens/`** — Four screens: `AmenityListScreen`, `AmenityDetailScreen`, `NavigationScreen`, `PreferencesScreen`
- **`ui/components/`** — Shared composables
- **`ui/theme/`** — Material3 theming with DFW brand colors (blue/orange palette)
- **`navigation/`** — `Screen` sealed class with typed route builders; `NavHost` in `MainActivity`
- **`di/`** — Hilt `AppModule` (SingletonComponent) binding repository implementations

### Key Libraries

- **UI**: Jetpack Compose + Material3
- **Navigation**: Jetpack Compose Navigation (single-activity)
- **DI**: Dagger Hilt
- **Database**: Room (persistence planned; preferences currently in-memory)
- **Networking**: Retrofit + OkHttp + Gson (wired to mock repository for now)
- **Build**: KSP for annotation processing (Hilt + Room), Java 17 / Kotlin JVM 17

### User Flow

```
AmenityListScreen → AmenityDetailScreen → NavigationScreen → AmenityListScreen
```
Preferences are accessible from `AmenityListScreen` and affect filtering and routing.

## Current Implementation Status (Iteration 1)

- Mock data source; no real backend connected
- Room database defined but not yet used for preferences persistence (planned Iteration 2)
- Accessibility preferences stored in ViewModel memory only
- Backend integration requires only changing the binding in `AppModule.kt`

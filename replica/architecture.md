# Architecture Design: Native Android StandBy Application

**Application Name:** StandBy Mode (Clean-room Android implementation)  
**Package:** `com.kunvarpreet.standbymode`  
**Reference Product:** StandBy Mode Pro (`br.com.zetabit.ios_standby`)  
**Target Platform:** Android 13+ (API level 33 minSdk, API level 37 targetSdk)  
**Author:** Antigravity Architect  
**Date:** 2026-10-18  

---

## 1. Executive Summary & Technology Stack

This document defines the architectural specification for an original, native Android ambient smart display application. Inspired by iOS StandBy and StandBy Mode Pro, the application activates when the device is placed horizontally on a charger, presenting a glanceable, touch-interactive dashboard with split widget panels, full-screen clocks, photo frame slideshows, and low-luminance night mode with OLED burn-in mitigation.

### Technology Stack Decisions

| Layer / Concern | Chosen Technology | Rationale & Architectural Fit |
|---|---|---|
| **Language** | Kotlin 2.4+ | Idiomatic, type-safe, first-class Coroutines/Flow, modern Android standard. |
| **UI Framework** | Jetpack Compose + Material 3 | Declarative, high-performance Canvas rendering for clocks, seamless animations, zero XML layouts. |
| **Architecture** | Single-Activity + MVVM / MVI (Unidirectional Data Flow) | Predictable state management (`UiState`, `UiIntent`, `UiEffect`), lifecycle-safe with `viewModelScope`. |
| **State & Concurrency** | Kotlin Coroutines & `StateFlow` / `SharedFlow` | Reactive stream processing for battery broadcasts, sensors, timers, and media events. |
| **Preferences Storage** | Jetpack DataStore (Preferences / Proto) | Asynchronous, transactional, coroutine-based replacement for SharedPreferences. |
| **Structured Storage** | Room Database (SQLite) | Local persistence for widget slot configurations, Space profiles, and persistent photo URIs. |
| **Navigation** | Jetpack Navigation Compose | Type-safe single-activity destination transitions (StandBy Canvas <-> Settings <-> Customizer). |
| **Screensaver Integration** | Android `DreamService` | Native OS hook for charging/docked ambient activation without background execution restrictions. |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`) | Modern, type-safe dependency management and build configuration. |

---

## 2. High-Level Component & Subsystem Architecture

The application is structured into 8 modular architectural subsystems to ensure clear separation of concerns, testability, and resilience against Android OS lifecycle interruptions:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           System & Hardware Triggers                        │
│   [Charging Broadcast / UsbReceiver]      [Orientation / Gyroscope]        │
│   [Ambient Light Sensor (Lux)]            [DreamService Framework Hook]     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 Core System & Trigger Engine (`core:trigger`)               │
│   - ChargingObserver                      - OrientationManager              │
│   - NightModeTriggerManager               - StandByDreamService             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 Domain State & ViewModels (`presentation:viewmodel`)        │
│   - StandByViewModel (Exposes StateFlow<StandByUiState>)                   │
│   - SettingsViewModel                     - CustomizerViewModel             │
│   - MediaControllerBridge                 - PhotoFrameManager               │
└───────────────────────┬───────────────────────────────┬─────────────────────┘
                        │                               │
                        ▼                               ▼
┌──────────────────────────────────────┐  ┌───────────────────────────────────┐
│     UI Display Layer (Compose)       │  │   Data & Persistence Layer        │
│  - StandByScaffold (Fullscreen Host) │  │  - StandByPreferencesDataStore    │
│  - DuoPaneLayout (Split Widget Slots)│  │  - Room Database (Spaces/Widgets) │
│  - FullscreenClockGallery            │  │  - ContentResolver (Photo URIs)   │
│  - SmartPhotoFrameSlideshow          │  └───────────────────────────────────┘
│  - QuickSettingsOverlay & Customizer │
│  - OLED BurnInShift Modifier         │
│  - RedMonochromeNightModifier        │
└──────────────────────────────────────┘
```

---

## 3. Proposed Package Tree

```
com.kunvarpreet.standbymode/
│
├── StandByApplication.kt               # Application class: DI init, channel setup
├── MainActivity.kt                     # Single Activity host: WindowFlags, immersive mode
│
├── core/
│   ├── common/                         # Coroutine dispatchers, result monads, extensions
│   │   ├── DispatcherProvider.kt
│   │   └── Extensions.kt
│   ├── datastore/                      # Jetpack DataStore preferences
│   │   ├── PreferencesKeys.kt
│   │   └── StandByPreferencesDataSource.kt
│   ├── database/                       # Room DB for spaces, widget configurations
│   │   ├── StandByDatabase.kt
│   │   ├── dao/
│   │   │   ├── SpaceDao.kt
│   │   │   └── WidgetSlotDao.kt
│   │   └── entity/
│   │       ├── SpaceEntity.kt
│   │       └── WidgetSlotEntity.kt
│   ├── model/                          # Domain models
│   │   ├── ClockStyle.kt
│   │   ├── WidgetType.kt
│   │   ├── SpaceProfile.kt
│   │   ├── NightModeConfig.kt
│   │   └── BatteryVitals.kt
│   └── protection/                     # Display protection utilities
│       ├── BurnInShiftModifier.kt      # Pixel shift logic
│       └── NightModeColorFilter.kt     # Monochromatic red ColorMatrix
│
├── engine/
│   ├── trigger/                        # Hardware trigger evaluation
│   │   ├── ChargingStateReceiver.kt    # ACTION_POWER_CONNECTED / DISCONNECTED
│   │   ├── OrientationObserver.kt      # Landscape detection
│   │   ├── AmbientLightObserver.kt     # Light sensor (lux) observer
│   │   └── StandByTriggerCoordinator.kt# Evaluates if app should launch
│   ├── dream/                          # Android Daydream screensaver integration
│   │   └── StandByDreamService.kt      # DreamService implementation
│   ├── media/                          # Media session listener
│   │   ├── MediaNotificationListener.kt# NotificationListenerService
│   │   └── MediaControllerBridge.kt    # PlaybackState & Metadata Flow
│   └── widgethost/                     # Android AppWidget hosting
│       ├── StandByAppWidgetHost.kt     # AppWidgetHost subclass
│       └── AppWidgetManagerHelper.kt   # Widget binding & config intent helpers
│
├── ui/
│   ├── theme/                          # Material 3 colors, typography, shapes
│   │   ├── Color.kt                    # Pure OLED black, deep red night palette
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/                     # Navigation Compose graph
│   │   ├── StandByNavGraph.kt
│   │   └── Screen.kt                   # Sealed destinations (Canvas, Settings, Onboarding)
│   ├── display/                        # Main StandBy Canvas
│   │   ├── StandByCanvasScreen.kt      # Primary root container
│   │   ├── HorizontalSpacePager.kt     # Horizontal swipe between Spaces
│   │   ├── QuickSettingsOverlay.kt     # Discreet pull-down menu
│   │   └── StandByViewModel.kt         # Main state holder
│   ├── layouts/                        # Panel layout engines
│   │   ├── DuoLayout.kt                # 50/50 split landscape container
│   │   ├── QuadLayout.kt               # 2x2 tablet container
│   │   └── WidgetStackView.kt          # Vertical swipeable stack
│   ├── clocks/                         # Clock renderers
│   │   ├── components/                 # Shared clock components
│   │   │   ├── FlipDigitCard.kt        # 3D split-flap mechanical digit
│   │   │   └── AnalogClockHands.kt     # Canvas-drawn hour/min/sec hands
│   │   ├── RetroFlipClockView.kt       # Mechanical flip clock
│   │   ├── DigitalBoldClockView.kt     # Oversized 2-line digital
│   │   ├── AnalogDialClockView.kt      # Classic dial watch
│   │   ├── DayDementiaClockView.kt     # High-legibility accessibility clock
│   │   ├── SolarPathClockView.kt       # Sun trajectory arc
│   │   └── NeonClockView.kt            # Glowing tube clock
│   ├── widgets/                        # Built-in widget panels
│   │   ├── BatteryVitalsWidget.kt      # Ring indicator, wattage, charging time
│   │   ├── CalendarMiniWidget.kt       # Monthly grid & agenda
│   │   ├── WeatherMiniWidget.kt        # Current temp, conditions, high/low
│   │   ├── MediaMiniWidget.kt          # Album art & media controls
│   │   ├── TimerStopwatchWidget.kt     # Bedside timer & stopwatch
│   │   └── AppWidgetHostContainer.kt   # Compose wrapper for RemoteViews
│   ├── photoframe/                     # Smart photo frame
│   │   ├── PhotoFrameScreen.kt         # Full-bleed slideshow
│   │   ├── FaceAwareCropModifier.kt    # ML Kit anchor centering
│   │   └── PhotoFrameViewModel.kt
│   ├── customizer/                     # Clock & widget configuration
│   │   ├── CustomizerSheet.kt          # Long-press edit sheet
│   │   ├── ColorPalettePicker.kt       # Swatch selector
│   │   └── WidgetPickerBottomSheet.kt  # Add widget picker
│   ├── settings/                       # Main Settings Screen
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── onboarding/                     # First launch walkthrough
│       ├── OnboardingScreen.kt
│       └── PermissionCard.kt
```

---

## 4. Dependencies & Version Catalog Plan

Based on the existing project configuration in `gradle/libs.versions.toml`, the following stable, production-ready AndroidX libraries are planned:

```toml
[versions]
agp = "9.5.0-alpha10"
kotlin = "2.4.21"
coreKtx = "1.19.1"
lifecycleRuntimeKtx = "2.11.0"
activityCompose = "1.13.0"
composeBom = "2026.09.00"
navigationCompose = "2.8.5"
datastore = "1.1.2"
room = "2.6.1"
ksp = "2.1.0-1.0.29"
coroutines = "1.9.0"
coil = "2.7.0"
mlkitFace = "16.1.7"

[libraries]
# Existing libraries remain active...
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
google-mlkit-face-detection = { group = "com.google.mlkit", name = "face-detection", version.ref = "mlkitFace" }
```

---

## 5. Data Models & Persistence Architecture

### A. DataStore Preferences (`StandByPreferences`)
Lightweight, frequently accessed configuration is stored via Jetpack DataStore:

```kotlin
data class StandByPreferences(
    val autoLaunchOnCharge: Boolean = true,
    val launchOnlyLandscape: Boolean = true,
    val launchOnlyWireless: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val defaultDisplaySpace: DisplaySpace = DisplaySpace.DUO_LAYOUT,
    // Night Mode
    val nightModeEnabled: Boolean = true,
    val nightModeTrigger: NightTrigger = NightTrigger.AMBIENT_SENSOR,
    val nightStartMinutes: Int = 22 * 60, // 22:00
    val nightEndMinutes: Int = 7 * 60,   // 07:00
    val nightBrightnessMultiplier: Float = 0.2f,
    // OLED Protection
    val burnInProtectionEnabled: Boolean = true,
    val pixelShiftIntervalSeconds: Int = 60,
    // Onboarding
    val onboardingCompleted: Boolean = false
)

enum class DisplaySpace { PHOTO_FRAME, DUO_LAYOUT, FULLSCREEN_CLOCK }
enum class NightTrigger { AMBIENT_SENSOR, SCHEDULED, ALWAYS_ACTIVE }
```

### B. Room Database Schema (`StandByDatabase`)
Structured configurations for custom widget stacks and Spaces:

```sql
-- Table: spaces (profiles for Bedside, Desk, Travel)
CREATE TABLE spaces (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    is_default INTEGER NOT NULL DEFAULT 0,
    active_clock_style TEXT NOT NULL DEFAULT 'RETRO_FLIP',
    clock_accent_color INTEGER NOT NULL DEFAULT 0xFFFF5722,
    clock_show_seconds INTEGER NOT NULL DEFAULT 1,
    clock_use_24h INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

-- Table: widget_slots (widgets assigned to left or right stack of a space)
CREATE TABLE widget_slots (
    id TEXT PRIMARY KEY NOT NULL,
    space_id TEXT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    pane_side TEXT NOT NULL CHECK (pane_side IN ('LEFT', 'RIGHT', 'GRID_0', 'GRID_1', 'GRID_2', 'GRID_3')),
    stack_order INTEGER NOT NULL,
    widget_type TEXT NOT NULL, -- 'CLOCK', 'BATTERY', 'CALENDAR', 'WEATHER', 'MEDIA', 'APPWIDGET'
    app_widget_id INTEGER,    -- nullable, non-null if widget_type == 'APPWIDGET'
    custom_config_json TEXT NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_widget_slots_space ON widget_slots(space_id, pane_side, stack_order);
```

---

## 6. Unidirectional State Flow (UDF) Architecture

The main Canvas screen is driven by `StandByViewModel`, which consolidates hardware sensor inputs, battery broadcasts, media playback, and user interactions into a single immutable `StandByUiState`:

```kotlin
data class StandByUiState(
    // Time & Date
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val is24Hour: Boolean = false,
    
    // Hardware & Environment State
    val isCharging: Boolean = false,
    val batteryLevel: Int = 100,
    val isFastCharging: Boolean = false,
    val isLandscape: Boolean = true,
    
    // Ambient & Protection State
    val isNightModeActive: Boolean = false,
    val isTemporaryBrightened: Boolean = false, // When user taps screen in night mode
    val pixelShiftOffset: Offset = Offset.Zero,  // (dx, dy) for burn-in prevention
    
    // Active Layout & Space
    val currentSpace: DisplaySpace = DisplaySpace.DUO_LAYOUT,
    val leftStackIndex: Int = 0,
    val rightStackIndex: Int = 0,
    val leftWidgets: List<WidgetSlotModel> = emptyList(),
    val rightWidgets: List<WidgetSlotModel> = emptyList(),
    val activeClockConfig: ClockFaceConfig = ClockFaceConfig.DEFAULT,
    
    // Subsystem States
    val mediaState: MediaPlaybackState = MediaPlaybackState.IDLE,
    val weatherState: WeatherState = WeatherState.Loading,
    
    // UI Dialogs
    val customizerTarget: CustomizerTarget? = null // Non-null when editing
)

sealed interface StandByUiIntent {
    data class OnHorizontalSpaceSwiped(val newSpace: DisplaySpace) : StandByUiIntent
    data class OnVerticalWidgetSwiped(val paneSide: PaneSide, val delta: Int) : StandByUiIntent
    data object OnScreenTapped : StandByUiIntent // Triggers temporary wake in night mode
    data class OnClockLongPressed(val target: CustomizerTarget) : StandByUiIntent
    data class OnUpdateClockColor(val colorArgb: Long) : StandByUiIntent
    data object OnDismissCustomizer : StandByUiIntent
}
```

---

## 7. Deep-Dive Subsystem Specifications

### Subsystem 1: Setup and Configuration UI
* **Onboarding Wizard (`OnboardingScreen`):**
  * Built using Compose `HorizontalPager`.
  * Step 1: Explain the docking concept with graphic illustration.
  * Step 2: "Appear on Top" (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`) with direct deep link.
  * Step 3: Battery Optimization (`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) with system dialog trigger.
  * Step 4: Notification / Media Listener (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
* **Settings Screen (`SettingsScreen`):**
  * Organizes preferences into categorized Material 3 cards: Charging Triggers, Display & Brightness, Night Mode, OLED Burn-in, and Space Management.
  * Live status chips showing whether Android permissions are currently active or revoked.

### Subsystem 2: StandBy Display and Clock Renderer
* **Immersive Host Activity:**
  * Uses `WindowCompat.setDecorFitsSystemWindows(window, false)` and `WindowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())`.
  * `WindowInsetsControllerCompat.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`.
* **Renderer Mechanics:**
  * High-efficiency redraw ticker: Clock uses `LaunchedEffect(Unit)` with `delay(1000L - (System.currentTimeMillis() % 1000L))` to sync exactly to system second changes.
  * Flip Digit: Uses two clipping half-rectangles (top and bottom) with a middle split seam. Rotates top half downward using `graphicsLayer { rotationX = animatedProgress * 180f; cameraDistance = 16 * density }`.
  * Analog Watch Dial: Drawn entirely on Compose `Canvas`. Precalculates angle radians `(hour * 30f + minute * 0.5f, minute * 6f, second * 6f)`.

### Subsystem 3: Clock Face Definitions and Customization
* **Model Definition:**
  ```kotlin
  sealed interface ClockFace {
      val id: String
      val displayName: String
      val supportedComplications: Set<ComplicationType>
      
      data class RetroFlip(val config: ClockFaceConfig) : ClockFace
      data class DigitalBold(val config: ClockFaceConfig) : ClockFace
      data class AnalogDial(val config: ClockFaceConfig) : ClockFace
      data class DayDementia(val config: ClockFaceConfig) : ClockFace
      data class SolarPath(val config: ClockFaceConfig) : ClockFace
  }
  ```
* **Customizer Sheet:**
  * Floating bottom sheet rendering color palette swatches, font pickers, and toggle switches for seconds/24h. Updates state reactively in memory with an instant live preview.

### Subsystem 4: Widget/Panel Layout Engine
* **Duo Layout:**
  * Uses `Row(modifier = Modifier.fillMaxSize())` with two equal children `Modifier.weight(1f).fillMaxHeight()`.
  * Each child embeds a `WidgetStackView` containing a `VerticalPager` or gesture-draggable slot.
* **Third-Party Widget Integration (`AppWidgetHost`):**
  * `StandByAppWidgetHost` instantiated with a unique host ID.
  * Wrapped in Compose using:
    ```kotlin
    AndroidView(
        factory = { context ->
            appWidgetHost.createView(context, appWidgetId, appWidgetInfo).apply {
                setAppWidget(appWidgetId, appWidgetInfo)
            }
        },
        update = { view -> /* handle size changes */ }
    )
    ```
  * Lifecycle integration: `appWidgetHost.startListening()` in `DisposableEffect`.

### Subsystem 5: Charging and Orientation Detection
* **Trigger Engine Workflow:**
  * `ChargingStateReceiver` listens for `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED`.
  * `OrientationObserver` uses `OrientationEventListener` to compute whether the device tilt is horizontal (`rotation in 60..120 || rotation in 240..300`).
  * If conditions match:
    1. If `StandByDreamService` is configured as the active screensaver, the OS automatically handles presentation.
    2. If launch-on-charge is enabled and permission is granted, app launches `MainActivity` with `Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP`.
* **Lockscreen & Awakeness:**
  * `MainActivity` sets:
    ```kotlin
    setShowWhenLocked(true)
    setTurnScreenOn(true)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    ```

### Subsystem 6: Photo Frame and Media Integrations
* **Photo Frame Slideshow:**
  * Uses Coil for memory-cached bitmap decoding.
  * Persists user-selected photo URIs via `takePersistableUriPermission`.
  * ML Kit Face Detection: When a bitmap loads, `FaceDetection.getClient()` detects faces. If detected, the bounding box center is used to set `Alignment(biasX, biasY)` in `ContentScale.Crop`, ensuring human faces are never cropped off screen.
* **Universal Media Controller:**
  * `MediaNotificationListener` listens for active `MediaSessionManager` tokens.
  * Extracts metadata: track name, artist, album art bitmap, and `PlaybackStateCompat`.
  * Mini widget provides Play/Pause and Skip buttons that send standard `KeyEvent` or `TransportControls` calls.

### Subsystem 7: Night Mode, Brightness, and Burn-in Mitigation
* **Ambient Light Evaluation:**
  * Registers `SensorEventListener` on `Sensor.TYPE_LIGHT`.
  * Applies an Exponential Moving Average (EMA) filter to lux readings to prevent flickering from minor room shadows.
  * Threshold: `< 5 lux` triggers Night Mode; `> 15 lux` returns to Daytime mode.
* **Low-Luminance Red Filter:**
  * Implemented as a reusable Compose modifier using `ColorMatrix`:
    ```kotlin
    val redMatrix = ColorMatrix(floatArrayOf(
        0.85f, 0f, 0f, 0f, 0f,   // Red channel retained
        0f,    0f, 0f, 0f, 0f,   // Green eliminated
        0f,    0f, 0f, 0f, 0f,   // Blue eliminated
        0f,    0f, 0f, 1f, 0f    // Alpha preserved
    ))
    Modifier.graphicsLayer { colorFilter = ColorFilter.colorMatrix(redMatrix) }
    ```
* **Burn-in Protection:**
  * `BurnInProtectionCoordinator` emits a new offset every 60 seconds:
    `Offset(dx = ((-6..6).random() * density).dp, dy = ((-6..6).random() * density).dp)`.
  * The entire layout applies `Modifier.offset(x = offset.x, y = offset.y)` to continuously alter active subpixel boundaries.

### Subsystem 8: Settings Persistence and Lifecycle Management
* All user modifications persist through repository interfaces backed by DataStore and Room.
* The application handles lifecycle callbacks (`onPause`, `onStop`, `onDestroy`) cleanly:
  * Releases camera/sensor listeners.
  * Stops `AppWidgetHost` listening to prevent battery drain.
  * Flushes pending analytics/state.

---

## 8. Platform Constraints & Reliability Matrix

| Platform Constraint | Technical Challenge | Architected Solution & Policy Compliance |
|---|---|---|
| **Android 10+ Background Activity Restrictions** | Apps targeting API 29+ cannot start activities directly from a broadcast receiver. | **1.** Primary recommendation is native `DreamService` (Daydream screensaver), which the OS automatically starts without restriction.<br>**2.** For direct charging launch, onboarding guides the user to grant `SYSTEM_ALERT_WINDOW` or sets high-priority `setFullScreenIntent`. |
| **OEM Background App Killers (Samsung/Xiaomi)** | Aggressive task killers terminate background receivers. | Guide user to whitelist the app under `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Use sticky foreground service when active. |
| **Lock Screen Security Boundaries** | Device may be PIN/fingerprint locked when plugged in. | `setShowWhenLocked(true)` and `setTurnScreenOn(true)` allows the StandBy Canvas to render above the lock screen without compromising user security. Interacting with widgets that open full apps prompts the OS unlock challenge. |
| **OLED Battery Drain & Heating** | Screen stays on for 8+ hours continuously. | Strict `#000000` AMOLED pure black background disables sub-pixel emissive LEDs. In night mode, brightness drops to hardware minimum. |
| **Media Controller Access Restrictions** | `MEDIA_CONTENT_CONTROL` is signature-only on Android. | Implemented via `NotificationListenerService`, which is the officially supported Android API for third-party media access. |
| **Third-Party AppWidget Crashes** | External widgets can crash, leak memory, or fail to render. | Wrapped in custom error boundaries (`ComposeErrorBoundary`); handles `AppWidgetHost` allocation failure gracefully without crashing the host app. |

---

## 9. Implementation Milestones

```
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 1: MVP Vertical Slice (The Core Loop)                            │
│ - Single Activity shell with landscape lock & FLAG_KEEP_SCREEN_ON      │
│ - Charging broadcast receiver & manual launch                          │
│ - Duo Layout engine with vertical swipe gesture                        │
│ - Built-in Retro Flip Clock & Big Digital Clock                        │
│ - Battery Vitals Widget (live percentage & charging indicator)         │
│ - Basic Night Mode red tint & OLED Pixel-Shift Coroutine Ticker        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 2: System Integration & Core Clocks                              │
│ - Android DreamService implementation (Daydream Screensaver)          │
│ - Analog Dial Watch & Day/Dementia high-legibility clock face          │
│ - DataStore preferences persistence for all settings                   │
│ - Fullscreen clock mode with horizontal space pager                    │
│ - Long-press in-place color and font customizer sheet                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 3: Widgets & Ambient Media Integrations                          │
│ - Calendar & Device Agenda Mini-Widget                                 │
│ - NotificationListenerService & Universal Media Player Widget          │
│ - AppWidgetHost bridge for third-party Android widgets                 │
│ - Ambient Light Sensor integration for automatic Night Mode            │
│ - Onboarding Wizard with deep links for system permissions             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 4: Smart Photo Frame & Polish                                    │
│ - Scoped Photo Picker integration with persistent URI access           │
│ - ML Kit Face Detection smart centering modifier                       │
│ - Quad Layout for Android tablets and large foldables                  │
│ - Solar Path & Neon glow clock faces                                   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Testing Strategy

1. **Unit Testing:**
   - Test `NightModeTriggerManager` with mocked light sensor lux streams and mock clocks.
   - Test `StandByPreferencesDataSource` migrations and default value loading.
   - Test `BurnInShiftCoordinator` math ensuring delta offsets never exceed max bounding boxes.
2. **Compose UI Previews & Screenshot Tests:**
   - Create `@Preview` variants for each clock face across Daytime, Night Mode (Red Filter), and Compact Phone Landscape vs Expanded Tablet sizes.
   - Verify that flipping digit animations render top and bottom cards without clipping seams.
3. **Hardware & Lifecycle Instrumentation Testing:**
   - Test `ACTION_POWER_CONNECTED` and `ACTION_POWER_DISCONNECTED` broadcast emissions using `adb shell dumpsys battery set ac 1` and `adb shell dumpsys battery reset`.
   - Test rotation change handling ensuring state does not reset when moving between `Surface.ROTATION_90` and `Surface.ROTATION_270`.
   - Validate that `FLAG_KEEP_SCREEN_ON` is cleared when unplugged or when app exits.

---

## 11. Next Step in Replica Workflow

The architecture specification is complete.  
Next recommended step: Activate `/replica-design` to produce the design tokens (color roles, typography scales, spacing grids, and component state specifications) before beginning code implementation.

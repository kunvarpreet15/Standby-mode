# Recon Map: StandBy Mode Pro (Android)

Scope: Core StandBy smart display experience — automatic charging detection & landscape launch, Always-On Display (AOD), side-by-side Duo layout with stacked interactive widgets, full-screen clock faces (Retroflip, Digital, Analog, Solar, Neon), Night Mode with low-luminance red tint, OLED burn-in pixel-shifting protection, device vitals/calendar/weather widgets, photo frame slideshow, and media controls.  
For: Native Android implementation in Kotlin using Jetpack Compose (original clean-room architecture).  
Date: 2026-10-18  

---

## 1. Sources & Evidence

| # | Source Type | URL / Reference | Notes / Key Insights |
|---|---|---|---|
| 1 | Official Website | `https://zetabitapps.com/standbymodepro` | Full feature catalogue, FAQ, JSON-LD Schema (`SoftwareApplication`), 100+ clock faces, burn-in pixel shift every minute, Duo & Quad layouts, face-aware photo cropping, Vibes radio. |
| 2 | Google Play Listing | `https://play.google.com/store/apps/details?id=br.com.zetabit.ios_standby` | Official app metadata (`br.com.zetabit.ios_standby`), 1M+ downloads, 4.6 rating (30.4K reviews), target device support (Android 7+ to Android 14+), screenshot inventory. |
| 3 | Package Manifest Analysis | `br.com.zetabit.ios_standby` APK inspection | Verified permissions: `WAKE_LOCK`, `DISABLE_KEYGUARD`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `BIND_APPWIDGET`, `READ_CALENDAR`, `ACCESS_COARSE_LOCATION`. |
| 4 | Official YouTube Demos | `https://www.youtube.com/watch?v=j_WQ_VCumfc` (Layout rearranging)<br>`https://www.youtube.com/watch?v=XeLjEQhaNvA` (Quad & Duo layout)<br>`https://www.youtube.com/watch?v=DfPZJ6OwDB8` (Screensaver mode)<br>`https://www.youtube.com/watch?v=8luu21Mgo1U` (Dementia clock)<br>`https://www.youtube.com/watch?v=WqJ6u3u9MeE` (Timer widget) | Real interaction flows: long-press to edit widgets, vertical flick to cycle stacked widgets, horizontal swipe to switch display modes, swipe-down-then-up gesture to reveal settings overlay. |
| 5 | Public Reviews & User Forum | `https://reddit.com/r/StandbyModeAndroid/` & Play Store user reviews | Common user pain points and edge cases: Android 10+ background launch blocking, OEM aggressive battery optimization (Samsung/Xiaomi), portrait accidental exits, OLED burn-in anxiety, Bluetooth dock triggers. |

---

## 2. Core Loop

> **The Core Loop:** When an Android phone is placed horizontally on a charger or nightstand dock, it immediately transforms into an elegant, always-on smart ambient display that shows glanceable time, weather, calendar, and music controls, automatically dimming into a sleep-friendly red hue at night with active OLED burn-in protection.

---

## 3. Screen Inventory

| ID | Screen Name | Route / How Reached | Purpose | Key Components | States Recorded |
|---|---|---|---|---|---|
| **S01** | **StandBy Display – Duo Layout** (Primary) | Auto-launch on charge + landscape, or manual launch | Split 50/50 side-by-side view with two independent interactive widget stacks | `DualPaneContainer`, `WidgetStackView` (Left & Right), `PixelShiftOffsetWrapper`, `GestureInterceptor` | Empty stack, filled (clock + calendar/weather), loading weather, media playing, active alarm indicator, horizontal page swipe. |
| **S02** | **StandBy Display – Fullscreen Clock Faces** | Horizontal swipe right from Duo layout (S01) | Full-screen, glanceable aesthetic clock face for bedside or desk viewing | `RetroFlipClock`, `DigitalBoldClock`, `AnalogDialClock`, `NeonClock`, `SolarClock`, `DementiaDayClock` | Normal daytime, low-power night tint, ticking/seconds active, 12h/24h formats, burn-in pixel shift. |
| **S03** | **StandBy Display – Smart Photo Frame** | Horizontal swipe left from Duo layout (S01) | Full-bleed digital photo frame slideshow of curated albums with clock pill | `PhotoSlideshowView`, `FaceCenteringCrop`, `CornerClockOverlay`, `FadeTransitionAnimator` | Empty (no permission / no photos picked), loading photo bitmap, transitioning photo, photo loaded. |
| **S04** | **StandBy Display – Ambient / Vibes Player** | Horizontal swipe or selecting Vibes mode | Relaxation desk display with lo-fi audio loop, crackling fireplace or ambient canvas | `AmbientVisualCanvas`, `AudioStreamController`, `MinimalClockHeader`, `SoundTogglePill` | Audio buffering, playing, muted, paused, offline error. |
| **S05** | **StandBy Display – Quad Layout** | Layout setting toggle on tablets / large foldables | 2x2 grid containing four simultaneous glanceable widgets | `QuadGridContainer`, 4x `WidgetStackView` | Grid populated, editing grid, tablet landscape responsive. |
| **S06** | **Widget & Face Customizer Sheet** | Long-press on any active widget or clock face | Interactive in-place editor for fonts, accent colors, second-hand toggle, widget ordering | `ColorPalettePicker`, `FontSelectorRow`, `WidgetReorderList`, `DoneCloseButton` | Active editing highlight, picker expanded, saving preference. |
| **S07** | **Widget Picker / Stack Manager** | Tapping "Add Widget (+)" from Customizer (S06) | Select built-in widgets (Battery, Weather, Calendar, Media, Timer) or 3rd-party Android widgets | `CategoryTabRow`, `BuiltinWidgetGrid`, `AppWidgetHostList`, `SearchInput` | Normal list, system permission prompt for `BIND_APPWIDGET`, empty search results. |
| **S08** | **Night Mode Display State** | Automatic (ambient sensor / time schedule) or manual tap | Applies low-luminance deep red/amber monochromatic color matrix to current display | `ColorMatrixRedFilter`, `BrightnessDimmerModifier`, `TouchToWakeTemporaryBrightener` | Dim red active, temporary touch brightening (5s timeout), ambient light transition. |
| **S09** | **Main Settings & Preferences** | Swipe down-then-up overlay gear icon, or launcher icon | Comprehensive app settings: triggers, screen rules, night mode schedule, burn-in settings | `PreferenceSectionList`, `SwitchPreference`, `TimePickerPreference`, `SliderPreference`, `PermissionStatusBanner` | Default, permissions missing warning banner, premium upsell badge. |
| **S10** | **Onboarding & Permission Wizard** | First app launch, or Settings > Support > Onboarding | Step-by-step setup explaining and requesting essential Android permissions | `PermissionStepPager`, `DeepLinkActionButton`, `IllustrationHero`, `GrantStatusBadge` | Step 1 (Overlay / Appear on top), Step 2 (Battery Unrestricted), Step 3 (Notification / Media access), Setup Completed. |

---

## 4. User Flows

### F01: Auto-Launch StandBy on Charger Plug-In & Landscape Tilt (Happy Path)
```
[User plugs in phone] 
       │
       ▼
BroadcastReceiver (ACTION_POWER_CONNECTED) 
       │ (checks: isLandscape == true? isWirelessOnly matched?)
       ▼
Activity launch over Lockscreen (S01 Duo Layout)
       │ (Window flags: FLAG_KEEP_SCREEN_ON, setShowWhenLocked(true), setTurnScreenOn(true))
       ▼
Active Ambient Display (S01 / S02) with OLED Burn-in Protection
```
* **Happy path clicks:** 0 (Fully automatic physical docking trigger).
* **Edge cases:**
  * Phone is plugged in while in vertical portrait orientation -> App remains in background or displays subtle notification; does not trigger full-screen until rotated to landscape.
  * Phone is unplugged -> `ACTION_POWER_DISCONNECTED` triggers finish / auto-dismiss back to home screen or lock screen.
  * Device has Keyguard locked -> App must display over keyguard without requiring user PIN/fingerprint until an interactive widget opens a third-party app.

### F02: Duo Layout Widget Flipping & Stack Navigation
```
S01 (Duo Layout) 
       │
       ├─► [Vertical swipe on Left Panel]  ──► Cycles Left Widget (Clock 1 -> Clock 2 -> Flip Clock)
       │
       └─► [Vertical swipe on Right Panel] ──► Cycles Right Widget (Calendar -> Weather -> Battery -> Music)
```
* **Happy path clicks:** 1 swipe gesture per widget flip.
* **Edge cases:** Stack has only 1 widget (overscroll bounce / no switch); gesture conflict between vertical widget swipe and horizontal space swipe (resolved via nested scroll priority).

### F03: Horizontal Paging Across Primary Display Spaces
```
[S03 Photo Frame] ◄──── Horizontal Swipe Left ──── [S01 Duo Layout] ──── Horizontal Swipe Right ────► [S02 Fullscreen Clock]
```
* **Happy path clicks:** 1 horizontal swipe between spaces.
* **Edge cases:** Infinite wrap-around or bounded snap-paging with subtle page indicators.

### F04: Customizing a Clock Face or Widget Slot
```
S01 or S02 
   │ [Long Press 500ms on element]
   ▼
S06 Customizer Overlay 
   │ (Selects accent color, changes font style, or toggles seconds hand)
   ▼
Taps "Done" or taps outside
   │ (Preferences updated in DataStore)
   ▼
Updated Live Clock / Widget in S01/S02
```
* **Happy path clicks:** 2 clicks (Long press -> tap color -> tap Done).

### F05: Adding an Installed 3rd-Party Android App Widget
```
S06 Customizer Overlay 
   │ [Tap "+ Add Widget"]
   ▼
S07 Widget Picker 
   │ [Select "App Widgets" tab -> Pick Spotify / Google Keep / Smart Home widget]
   ▼
System BIND_APPWIDGET permission dialog (if first time)
   │ [User confirms "Create"]
   ▼
AppWidgetHost allocates WidgetId and embeds AppWidgetHostView into S01 stack
```
* **Happy path clicks:** 3 clicks.
* **Edge cases:** Widget configuration activity required by the 3rd-party widget (e.g. choosing a note in Keep or city in weather); user denies widget binding permission.

### F06: Night Mode Automatic or Manual Transition
```
Ambient Light Sensor drops below 5 lux (or Night Schedule triggers)
       │
       ▼
S08 Night Mode State activates on top of current screen
       │ (Smooth crossfade to monochrome deep red #FF2A2A with black background)
       ▼
Display runs at minimum hardware screen brightness
       │ [User taps screen at 3 AM]
       ▼
Temporarily increases brightness to 15% for 5 seconds -> reverts to dim red.
```

### F07: First-Time Setup & Permission Wizard
```
User installs app -> Launches S10 Onboarding Wizard
       │
       ├─► Step 1: "Appear on Top" -> Deep-links to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
       ├─► Step 2: "Ignore Battery Optimization" -> Deep-links to Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
       ├─► Step 3: "Media & Notification Access" -> Deep-links to NotificationListenerSettings
       └─► Step 4: "Enable Auto-Launch" -> Saves preference
       ▼
User completes onboarding -> Ready for docking.
```
* **Happy path clicks:** 4 setup steps with clear deep links.

---

## 5. UI & Component Inventory

| Component Name | Description & Variants | Supported States | Used On |
|---|---|---|---|
| `DualPaneContainer` | Responsive horizontal split layout (50/50 on phone landscape; adaptive spacing) | Landscape, Portrait fallback, Compact, Expanded | S01 |
| `WidgetStackCard` | Rounded card container hosting a circular stack of widgets with vertical swipe gesture | Idle, Swiping, EditMode (pulsing / jiggle), Empty | S01, S05 |
| `FlipDigitView` | Mechanical split-flap card rendering top/bottom halves with 3D rotation flip animation | Idle, Flipping (180deg vertical rotation), Settled | S01, S02 |
| `AnalogClockDial` | Canvas-drawn analog watch face (ticks, numbers, hour/minute/second hands, date complication) | Smooth sweep, ticking second, daytime palette, night red | S01, S02 |
| `DigitalClockBold` | Oversized 2-line digital time (HH on top, MM on bottom, or single line HH:MM:SS) | 12-hour, 24-hour, with seconds, red night mode | S01, S02 |
| `DayDementiaClock` | Accessibility-first text clock: "Wednesday Morning", full date, clear large time | Default, High contrast, Night mode | S02 |
| `SolarPathClock` | Graphic arc showing sun position relative to horizon with sunrise/sunset hours | Day, Dusk, Night (moon arc), Low power | S02 |
| `CalendarMiniWidget` | Glanceable current-month mini calendar with current day circled and next agenda event | Loaded, No events, Calendar permission missing | S01, S05 |
| `WeatherMiniWidget` | Current temperature, weather icon, high/low, rain percentage, location name | Loading, Loaded, Error (offline / location disabled) | S01, S05 |
| `BatteryVitalsWidget` | Circular progress ring with charging wattage/speed, battery %, time until full | Charging (fast/slow/wireless), Full, Discharging | S01, S05 |
| `MediaMiniWidget` | Compact music player showing album thumbnail, title, artist, play/pause, next/prev | Idle (no media), Active playing, Paused, Seeking | S01, S05 |
| `BurnInShiftModifier` | Custom Jetpack Compose Modifier translating canvas offset by ±4px to ±8px every 60s | Active shift (invisible to user), Reset on wake | All StandBy screens |
| `NightModeFilterModifier`| Compose GraphicsLayer ColorFilter transforming all non-black pixels into monochromatic deep red | Inactive, Active (Red), Waking (temporary brighten) | All StandBy screens |
| `QuickSettingsBar` | Discreet swipe-down overlay containing brightness slider, night mode toggle, settings gear | Hidden, Revealed on gesture, Auto-dismiss (3s) | S01, S02, S03, S04 |
| `ColorPalettePicker` | Horizontal row of color swatches (iOS Orange, Cyber Cyan, Neon Green, Pure White, Crimson) | Unselected, Selected (with checkmark ring) | S06 |

---

## 6. Inferred Data Model

```
StandbyPreferences
    autoLaunchOnCharge: Boolean (default: true)
    launchOnlyLandscape: Boolean (default: true)
    launchOnlyWireless: Boolean (default: false)
    keepScreenAwake: Boolean (default: true)
    burnInProtectionEnabled: Boolean (default: true)
    pixelShiftIntervalSeconds: Int (default: 60)
    nightModeEnabled: Boolean (default: true)
    nightModeTrigger: Enum (AMBIENT_SENSOR | SCHEDULE | ALWAYS_NIGHT)
    nightModeStartTime: LocalTime (default: 22:00)
    nightModeEndTime: LocalTime (default: 07:00)
    nightModeRedIntensity: Float (0.0 to 1.0, default: 0.85)
    defaultSpaceIndex: Int (default: 0)
    evidence: Public FAQ, YouTube Settings walkthroughs, Store descriptions
    confidence: high

SpaceProfile
    id: String (UUID)
    name: String ("Bedside", "Desk", "Minimal")
    leftWidgetStack: List<WidgetSlotConfig>
    rightWidgetStack: List<WidgetSlotConfig>
    activeClockFaceId: String ("RETRO_FLIP", "DIGITAL_BOLD", etc.)
    evidence: Zetabit website "Spaces — separate profiles for home, work and focus"
    confidence: high

WidgetSlotConfig
    slotId: String
    widgetType: Enum (BUILTIN_CLOCK, BUILTIN_CALENDAR, BUILTIN_WEATHER, BUILTIN_BATTERY, BUILTIN_MEDIA, BUILTIN_TIMER, THIRD_PARTY_APPWIDGET)
    appWidgetId: Int? (if 3rd party widget)
    customSettingsJson: String (colors, complication selections)
    evidence: Video demo of Duo layout customizer & 3rd-party widget hosting
    confidence: high

ClockFaceConfig
    style: Enum (RETRO_FLIP, DIGITAL_BOLD, ANALOG_CLASSIC, NEON_GLOW, SOLAR_ARC, DEMENTIA_ACCESSIBILITY)
    primaryColorArgb: Long
    secondaryColorArgb: Long
    fontFamily: String ("Default", "Monospace", "Condensed", "Serif")
    showSeconds: Boolean
    use24Hour: Boolean
    evidence: App screenshots, YouTube clock customization demos
    confidence: high

PhotoFrameConfig
    albumUriList: List<String> (persisted content URIs)
    transitionIntervalSeconds: Int (default: 15)
    faceDetectionCentering: Boolean (default: true)
    showClockOverlay: Boolean (default: true)
    evidence: Website FAQ "Smart photo frame with face-aware cropping"
    confidence: high
```

### Relationships:
* `StandbyPreferences` 1 ── 1 `SpaceProfile` (active profile)
* `SpaceProfile` 1 ── n `WidgetSlotConfig` (left and right stacks)
* `SpaceProfile` 1 ── 1 `ClockFaceConfig`
* `SpaceProfile` 1 ── 1 `PhotoFrameConfig`

---

## 7. Feature Matrix Summary

*(See complete spreadsheet in [features.csv](file:///home/lordshadow/Projects/AndroidStudioProjects/StandByMode/replica/features.csv))*

* **Must (16 features):** Core charging broadcast receiver, landscape orientation trigger, `FLAG_KEEP_SCREEN_ON`, Always-On black background, Duo side-by-side layout, vertical stack flipping, horizontal display space paging, Retro flip clock, bold digital clock, analog clock, night mode red tint, pixel-shift burn-in protection, battery vitals widget, calendar agenda widget, long-press color/font customizer, settings screen, permission onboarding wizard.
* **Should (11 features):** System `DreamService` screensaver integration, photo frame slideshow, on-device ML Kit face-centering crop, live weather widget, universal media playback controller (`NotificationListenerService`), fullscreen timer & stopwatch, solar path clock, neon glow clock, matrix code clock, dementia accessibility clock, third-party Android widget hosting (`AppWidgetHost`), ambient light sensor trigger.
* **Could (4 features):** Quad 4-widget layout for tablets, vibes ambient lo-fi radio, multi-profile Spaces, in-app billing / premium unlock.
* **Skip (2 features):** Synchronized music lyrics (requires proprietary licensing API), custom YouTube background streaming loops (violates YouTube TOS / out of scope for smart clock).

---

## 8. Android APIs, Permissions, Platform Restrictions & Lifecycle

### A. Android 10+ (API 29+) Background Launch Restrictions & Workarounds
* **Platform Restriction:** Android 10 and above strictly blocks starting an Activity directly from background `BroadcastReceiver`s (e.g. `ACTION_POWER_CONNECTED`).
* **Required Implementation:**
  1. **Primary Mechanism (Screen Saver / Daydream):** Register an Android `DreamService` in the manifest. The Android operating system automatically starts the `DreamService` when the device is placed on a dock or plugged into a charger. This requires **zero overlay hacks** and is 100% compliant with Google Play policies.
  2. **Secondary Mechanism (Active Charging Detection):** 
     * Request `SYSTEM_ALERT_WINDOW` ("Display over other apps") in onboarding.
     * When `ACTION_POWER_CONNECTED` fires, if the screen is already on and rotated in landscape, launch `MainActivity` using `FLAG_ACTIVITY_NEW_TASK`.
     * If the screen is locked, post a high-priority Notification with `setFullScreenIntent(pendingIntent, true)` which Android allows for alarm/dock style interactions.

### B. Display Over Keyguard & Screen Awake
* **Required APIs:**
  * In `MainActivity.onCreate()`:
    ```kotlin
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    ```
  * In Jetpack Compose: Enforce pure black background (`Color(0xFF000000)`) so OLED panels turn off backlight LEDs on black pixels.

### C. OLED Burn-in Mitigation Engineering
* **Mechanism 1 (Pixel Shifting):** An idle timer shifts the Compose canvas draw offset every 60 seconds by a pseudo-random delta `(dx, dy) ∈ [-8dp, +8dp]`. Over hours of display, static clock segment borders do not continuously burn the same OLED sub-pixels.
* **Mechanism 2 (Low-Luminance Red Filter):** In night mode, a `ColorMatrix` maps all output colors to `(R * 0.8, 0, 0)` with zero blue and zero green channels. Red OLED sub-pixels operate at the lowest photon energy and degrade significantly slower than blue OLED sub-pixels, while preserving human rhodopsin (dark adaptation).

### D. Third-Party Android Widget Hosting (`AppWidgetHost`)
* **API Requirements:** `AppWidgetManager`, `AppWidgetHost`.
* **Jetpack Compose Integration:** Compose does not have a native RemoteViews renderer. Must wrap `AppWidgetHostView` in an `AndroidView { context -> host.createView(context, appWidgetId, appWidgetInfo) }`.
* **Lifecycle:** `appWidgetHost.startListening()` in `onStart()`, `appWidgetHost.stopListening()` in `onStop()`. Handle `appWidgetHost.deleteAppWidgetId()` when a user removes a widget from the stack.

### E. Universal Media Controls
* **API Requirements:** Cannot use `MEDIA_CONTENT_CONTROL` directly (system-only permission).
* **Solution:** Implement a `NotificationListenerService`. Android grants active media controller session tokens (`MediaSessionManager.getActiveSessions(componentName)`) to any app granted notification listener access.
* **Lifecycle:** Listens for `PlaybackState` changes and `MediaMetadata` changes to extract album artwork bitmap, title, artist, and send play/pause/skip transport controls.

### F. Photo Frame & Smart Face Cropping
* **Photo Access:** Use modern Android Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia()`) which requires **zero storage permissions**.
* **Persisting URIs:** Must call `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` to ensure photos can still be loaded after device reboots.
* **Smart Centering:** Use Google ML Kit Face Detection (`com.google.mlkit:face-detection`) to calculate the center of mass of detected faces and calculate the Compose image crop anchor point `Alignment(biasX, biasY)` instead of generic center-crop.

---

## 9. Verified Observations vs. Assumptions & Unknowns

### Verified Observations (Ground Truth)
* **Website & Store Data:** Confirmed 100+ clock faces, Duo & Quad widget layouts, pixel-shifting burn-in protection occurring on a 1-minute cycle, red night mode, photo frame with Google Photos/local gallery integration, and auto-start on charging.
* **Manifest & Package Inspection:** Confirmed presence of `DreamService` registration, `AppWidgetHost` usage for third-party widgets, `NotificationListenerService` for media control, and `SYSTEM_ALERT_WINDOW` for overlay launching.
* **User Gesture Model:** Confirmed through official YouTube tutorials: vertical swipe cycles stack widgets; horizontal swipe switches spaces; long-press opens customization modal; swipe down-then-up reveals quick settings.

### Assumptions & Inferred Details
* **Weather Provider:** StandBy Mode Pro likely queries Open-Meteo or OpenWeatherMap with coordinates obtained from `FusedLocationProviderClient`.
* **Vibes Streams:** Vibes radio streams are likely public royalty-free Icecast/HLS audio streams and embedded webview/video loops.
* **Face Centering:** Assumed to use Google ML Kit on-device face detector or OpenCV Haar cascades; ML Kit is the standard Android on-device solution.

### Unknowns
* Exact server endpoints used by Zetabit for weather or custom cloud configurations (irrelevant for our clean-room implementation).
* Exact internal algorithm for dynamic auto-arranging widget sizes on varied tablet aspect ratios.

---

## 10. Out of Scope (What Cannot or Should Not Be Cloned)
1. **Proprietary Music Streaming SDKs & Real-time Lyrics API:** StandBy Mode Pro does not host music itself; our clone will use Android's public `MediaController` / `NotificationListenerService` to control external players. We will not license closed lyrics databases.
2. **YouTube Video Scraping / Looping:** Bundling unofficial YouTube scrapers violates YouTube Terms of Service and risks Google Play bans. Our clone will focus on local ambient animations or procedural Canvas shaders (crackling fire, starfield, lofi rain).
3. **Zetabit Server Infrastructure:** The clone must be 100% client-driven and offline-first, using public weather APIs and local storage (`DataStore`).

---

## 11. Sizing & Smallest Useful Version (MVP / Phase 1)

### Complexity Estimate
* **Total Screens:** 10 screens / states.
* **Total User Flows:** 7 distinct flows.
* **Overall Sizing:** **M (3–4 weeks for complete parity)**.

### Smallest Useful Version (Phase 1 MVP - 1 Weekend Build)
To establish the core loop without bloat, the initial vertical slice will consist of:
1. **App Shell & Orientation/Docking Host:** `MainActivity` with landscape orientation lock, `FLAG_KEEP_SCREEN_ON`, and `setShowWhenLocked(true)`.
2. **Duo Layout Engine (S01):** Dual-pane horizontal view with vertical swipe gesture per pane.
3. **Core Built-in Widgets:**
   - Left Slot: Retro Flip Clock & Big Digital Clock.
   - Right Slot: Battery Vitals widget (real-time percentage & charging indicator) & Device Calendar widget.
4. **Fullscreen Clock Mode (S02):** Fullscreen Retro Flip Clock & Bold Digital Clock with horizontal swipe transition.
5. **Night Mode Filter (S08):** Instant toggle or scheduled monochrome red color matrix filter.
6. **Burn-in Protection:** Compose canvas offset pixel-shifter running on a 60-second coroutine ticker.
7. **Settings & Customizer (S06, S09):** In-place long-press color picker and basic toggle settings.

---

## 12. Next Step
The reconnaissance map and feature matrix are complete.  
Next recommended step in the Replica workflow: Run `/replica-architect` to define the clean architecture stack, state management, Compose component hierarchy, and DataStore schema.

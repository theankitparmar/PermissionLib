# PermissionLib

A lightweight, lifecycle-aware Android runtime permission library written in Kotlin.
Replaces 50+ lines of boilerplate with a clean fluent API — with full coroutine support.

**Author:** Ankit Parmar
**License:** MIT

---

## Features

- Fluent builder API — readable one-liners
- Lifecycle-aware — automatic cleanup, no memory leaks
- Three callback styles: lambdas, aggregate `onResult {}`, or `PermissionCallback` interface
- Coroutine / suspend support via `requestSuspend()`
- Auto retry on soft-denied permissions (`retryOnDenied()`)
- Customisable permanently-denied dialog (title, message, colors, font size)
- Rationale dialog before the system prompt
- Pre-defined permission groups (Camera, Location, Storage, Bluetooth, etc.)
- Handles configuration changes safely (dialog state persisted via Bundle)
- API 24–36 support with API-level adaptive permission groups

---

## Setup

### 1. Add JitPack to your project

In your **root** `settings.gradle.kts` (or `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency

In your **app** `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.theankitparmar:permissionlib:1.0.56")
}
```

---

## Usage

### Basic — single permission

```kotlin
PermissionManager.with(this)   // 'this' = Activity or Fragment
    .permissions(Manifest.permission.CAMERA)
    .onGranted { startCamera() }
    .onDenied  { showToast("Camera permission denied") }
    .onPermanentlyDenied { /* settings dialog shown automatically */ }
    .request()
```

### Multiple permissions

```kotlin
PermissionManager.with(this)
    .permissions(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    .onGranted { startVideoCall() }
    .onDenied  { showExplanation() }
    .request()
```

### Using a pre-defined permission group

```kotlin
PermissionManager.with(this)
    .permissions(*PermissionGroup.CAMERA_AND_MIC)
    .onGranted { startVideoCall() }
    .request()
```

Available groups: `CAMERA`, `MICROPHONE`, `CAMERA_AND_MIC`, `LOCATION`,
`LOCATION_BACKGROUND`, `STORAGE`, `STORAGE_IMAGES`, `PHONE`, `CONTACTS`,
`CALENDAR`, `BLUETOOTH`, `NOTIFICATIONS`, `BODY_SENSORS`, `ACTIVITY_RECOGNITION`

### Aggregate result callback

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    .onResult { results ->
        when {
            results.allGranted          -> proceed()
            results.anyDenied           -> showRationale()
            results.anyPermanentlyDenied -> openSettings()
        }
    }
    .request()
```

### Coroutine / suspend

```kotlin
lifecycleScope.launch {
    val results = PermissionManager.with(this@MainActivity)
        .permissions(Manifest.permission.CAMERA)
        .requestSuspend()

    if (results.allGranted) startCamera()
    if (results.anyPermanentlyDenied) showSettingsHint()
}
```

### Structured callback interface

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
    .callback(object : PermissionCallback {
        override fun onGranted(permissions: List<String>)          { showMap() }
        override fun onDenied(permissions: List<String>)           { showBanner() }
        override fun onPermanentlyDenied(permissions: List<String>) { showSettings() }
    })
    .request()
```

### Rationale dialog (shown before system prompt)

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.ACCESS_FINE_LOCATION)
    .rationale(
        title   = "Location Required",
        message = "We use your location to show nearby results."
    )
    .onGranted { loadNearby() }
    .request()
```

### Auto-retry on soft denial

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.RECORD_AUDIO)
    .retryOnDenied()          // re-requests once if user accidentally dismisses
    .onGranted { startRecording() }
    .onDenied  { showExplanation() }
    .request()
```

### Custom permanently-denied dialog

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.CAMERA)
    .dialogConfig(
        title         = "Camera Access Needed",
        message       = "Please enable Camera in App Settings to continue.",
        positiveText  = "Go to Settings",
        negativeText  = "Maybe Later",
        positiveColor = Color.parseColor("#6200EE")
    )
    .onGranted { startCamera() }
    .request()
```

Pass a `DialogConfig` object for full control:

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.CAMERA)
    .dialogConfig(
        DialogConfig(
            title               = "Camera Access",
            message             = "Required to scan QR codes.",
            positiveButtonText  = "Enable",
            negativeButtonText  = "Cancel",
            positiveButtonColor = Color.parseColor("#4CAF50"),
            backgroundColor     = Color.parseColor("#FAFAFA"),
            isCancelable        = false
        )
    )
    .request()
```

### Disable the default permanently-denied dialog

```kotlin
PermissionManager.with(this)
    .permissions(Manifest.permission.CAMERA)
    .disableDefaultDialog()
    .onPermanentlyDenied { myCustomFlow() }
    .request()
```

---

## Static helpers

```kotlin
// Quick permission check (no request)
if (PermissionManager.isPermissionGranted(context, Manifest.permission.CAMERA)) {
    startCamera()
}

// Check multiple permissions
if (PermissionManager.areAllGranted(context, listOf(
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))) {
    startVideoCall()
}

// Detect permanently denied state (to show custom UI)
if (PermissionManager.isPermanentlyDenied(activity, Manifest.permission.CAMERA)) {
    showSettingsBanner()
}

// Navigate to system settings
PermissionManager.openAppSettings(context)
```

### Context extension functions

```kotlin
// Single permission
if (context.isGranted(Manifest.permission.CAMERA)) startCamera()

// Multiple permissions
if (context.areAllGranted(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) {
    startVideoCall()
}

// Get denied subset
val missing = context.filterDenied(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION
)
```

### Per-permission sealed state via `toResultList()`

Convert an aggregate result into a per-permission list of typed states:

```kotlin
lifecycleScope.launch {
    val results = PermissionManager.with(this@MainActivity)
        .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        .requestSuspend()

    results.toResultList().forEach { state ->
        when (state) {
            is PermissionResult.Granted           -> enableFeature(state.permission)
            is PermissionResult.Denied            -> showRationale(state.permission)
            is PermissionResult.PermanentlyDenied -> showSettingsPrompt(state.permission)
        }
    }
}
```

---

## PermissionResults

```kotlin
data class PermissionResults(
    val granted:           List<String>,
    val denied:            List<String>,
    val permanentlyDenied: List<String>
) {
    val allGranted:           Boolean  // true if denied and permanentlyDenied are empty
    val anyDenied:            Boolean  // true if at least one was soft-denied
    val anyPermanentlyDenied: Boolean  // true if at least one was permanently denied
}
```

## PermissionResult

Sealed class for per-permission typed states, obtained via `PermissionResults.toResultList()`:

```kotlin
sealed class PermissionResult {
    data class Granted(val permission: String)           : PermissionResult()
    data class Denied(val permission: String)            : PermissionResult()
    data class PermanentlyDenied(val permission: String) : PermissionResult()
}
```

---

## DialogConfig defaults

| Property | Default |
|---|---|
| `title` | `"Permission Required"` |
| `message` | `"This permission is required for the app to function properly. Please grant it from App Settings."` |
| `positiveButtonText` | `"Open Settings"` |
| `negativeButtonText` | `"Cancel"` |
| `positiveButtonColor` | `#2196F3` |
| `negativeButtonColor` | `#757575` |
| `backgroundColor` | `White` |
| `titleFontSize` | `18sp` |
| `messageFontSize` | `14sp` |
| `isCancelable` | `true` |

---

## Requirements

| Property | Value |
|---|---|
| Min SDK | 24 (Android 7.0) |
| Compile SDK | 36 |
| Kotlin | 2.0+ |
| AGP | 8.x |

---

## License

```
MIT License

Copyright (c) 2024 Ankit Parmar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

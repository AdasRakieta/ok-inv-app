---
name: systematic-debugging
description: Use when encountering any bug, test failure, or unexpected Android behavior, before proposing fixes
---

# Systematic Debugging for Android

## Overview

Random fixes waste time and create new bugs. Quick patches mask underlying issues.

**Core principle:** ALWAYS find root cause before attempting fixes.

## The Iron Law

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

## When to Use

Use for ANY Android technical issue:
- App crashes or ANRs (Application Not Responding)
- Test failures (unit, instrumented, UI)
- UI rendering issues
- Memory leaks or performance problems
- Build or Gradle failures
- Database migration errors
- Bluetooth/printer connection issues
- Camera/barcode scanning failures

## The Four Phases

### Phase 1: Root Cause Investigation

**BEFORE attempting ANY fix:**

1. **Read Error Messages and Logcat**
   ```bash
   # View logcat for app only
   adb logcat -s "ok.inv:*" --pid=$(adb shell pidof -s com.ok.inv)
   
   # Full logcat with filtering
   adb logcat | grep -E "(AndroidRuntime|FATAL|ERROR)"
   
   # Clear and watch fresh
   adb logcat -c && adb logcat
   ```

2. **Reproduce Consistently**
   - Can you trigger on emulator AND physical device?
   - Does it happen on all Android versions (API 26-31)?
   - Specific device models? (Check device-specific quirks)
   - Does it occur in debug AND release builds?

3. **Check Recent Changes**
   ```bash
   # Recent commits
   git --no-pager log --oneline -10
   
   # Changes in specific file
   git --no-pager log --oneline -- app/src/main/java/com/ok/inv/Scanner.kt
   
   # Diff last commit
   git --no-pager diff HEAD~1
   ```

4. **Android-Specific Diagnostics**

   **For Crashes:**
   - Stack trace in logcat
   - ProGuard mapping (if release build)
   - Thread name (main thread vs background)
   
   **For Performance:**
   ```bash
   # CPU profiling
   adb shell am profile start com.ok.inv /sdcard/profile.trace
   adb shell am profile stop com.ok.inv
   
   # Memory dump
   adb shell am dumpheap com.ok.inv /sdcard/heap.hprof
   ```
   
   **For Database Issues:**
   ```bash
   # Inspect database
   adb shell "run-as com.ok.inv cat /data/data/com.ok.inv/databases/equipment.db" > equipment.db
   sqlite3 equipment.db ".schema"
   ```
   
   **For Lifecycle Issues:**
   - Check which lifecycle events triggered
   - Fragment transaction state
   - Activity recreation scenarios

### Phase 2: Android Pattern Analysis

1. **Find Working Examples**
   - Look for similar working code in codebase
   - Check Android documentation samples
   - Review project's existing patterns

2. **Compare Against Android Best Practices**
   - Android Developers documentation
   - Jetpack components usage
   - Material Design guidelines
   - Architecture Components patterns

3. **Common Android Gotchas**
   - Main thread violations (NetworkOnMainThreadException)
   - Context leaks (holding Activity references)
   - Fragment lifecycle mismatches
   - Bluetooth connection threading
   - Camera permissions and lifecycle

### Phase 3: Hypothesis and Testing

1. **Form Hypothesis**
   Example: "I think the crash occurs because CameraX is initialized before permissions are granted"

2. **Test Minimally**
   ```kotlin
   // Add logging to verify hypothesis
   Log.d("DEBUG", "Permission state: ${hasPermission}, Camera state: ${isCameraReady}")
   ```

3. **Verify with Android Tools**
   ```bash
   # Layout inspection
   adb shell dumpsys activity top
   
   # Activity stack
   adb shell dumpsys activity activities
   
   # View hierarchy
   adb shell uiautomator dump && adb pull /sdcard/window_dump.xml
   ```

### Phase 4: Implementation

1. **Create Failing Test**
   ```kotlin
   @Test
   fun `scanner crashes when permissions denied`() {
       // Arrange
       every { permissionChecker.hasPermission() } returns false
       
       // Act & Assert
       assertThrows<SecurityException> {
           scannerViewModel.initializeCamera()
       }
   }
   ```

2. **Implement Fix**
   ```kotlin
   fun initializeCamera() {
       if (!permissionChecker.hasPermission()) {
           throw SecurityException("Camera permission required")
       }
       // Initialize camera
   }
   ```

3. **Verify Fix**
   - Test passes
   - Manual testing on device
   - Check for regressions
   - Verify on different Android versions

## Android-Specific Red Flags

| Symptom | Likely Root Cause |
|---------|-------------------|
| `NetworkOnMainThreadException` | Network call on UI thread, missing coroutine |
| `IllegalStateException: commit after onSaveInstanceState` | Fragment transaction timing issue |
| App works in debug, crashes in release | ProGuard rules stripping needed code |
| Bluetooth fails intermittently | Threading issue, missing permissions, or bond state |
| Database migration fails | Missing migration path or schema mismatch |
| Memory leak | Activity/Fragment context held in ViewModel or singleton |
| ANR | Long operation on main thread |

## Project-Specific Debugging Commands

```bash
# Build and deploy debug
./gradlew quickDeploy

# View build errors
cat build_output.txt

# Run tests with logging
./gradlew test --info

# Check ProGuard rules
cat app/proguard-rules.pro

# Inspect APK
./gradlew assembleDebug && apkanalyzer dex packages app/build/outputs/apk/debug/app-debug.apk

# ADB device info
adb shell getprop ro.build.version.sdk  # API level
adb shell getprop ro.product.model       # Device model
```

## Common Android Debugging Techniques

**Enable Debug Logging:**
```kotlin
// In Application class
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}

// Usage
Timber.d("Scanner state: %s", scannerState)
Timber.e(exception, "Printer connection failed")
```

**Inspect ViewModel State:**
```kotlin
viewModel.uiState.observeForever { state ->
    Log.d("DEBUG", "UI State changed: $state")
}
```

**Monitor Coroutines:**
```kotlin
viewModelScope.launch {
    Log.d("DEBUG", "Coroutine started on ${Thread.currentThread().name}")
    // Operation
    Log.d("DEBUG", "Coroutine completed")
}
```

## Red Flags - STOP and Follow Process

- Proposing fix without checking logcat
- "Just add try-catch" without understanding exception
- Changing multiple things at once
- Not testing on physical device
- Skipping ProGuard/R8 testing for release
- Ignoring ANR or memory warnings

## Quick Reference

| Phase | Android-Specific Actions |
|-------|-------------------------|
| **1. Root Cause** | Check logcat, reproduce on device, inspect stack trace |
| **2. Pattern** | Review Android docs, check similar working code |
| **3. Hypothesis** | Add logging, use Android debugging tools |
| **4. Implementation** | Write test, implement fix, verify on device |

## When to Question Architecture (3+ Failed Fixes)

If you've tried 3+ fixes and each reveals new problems:
- **Camera integration** - May need CameraX migration
- **Bluetooth connectivity** - May need state machine refactor
- **Database migrations** - May need schema redesign
- **Fragment lifecycle** - May need Navigation Component

STOP and discuss architectural changes with team.

---

**Adapted from:** obra/superpowers systematic-debugging skill
**Project:** ok-inv-app (Android Kotlin)

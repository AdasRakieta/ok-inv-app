# QR Generation & Scanning (Implementation Notes)

This document describes where QR-related code lives, how it works, and how to verify it manually and with tests.

## Overview
- Each warehouse location has a stable `qrUid` (UUID) stored in the Room entity `WarehouseLocationEntity`.
- QR payload encoded: `invapp://location/{qrUid}` — used as deep link and scanner payload.
- Images are saved to Android `MediaStore` on Android 10+; fallback writes PNG files to the project's `exports` directory via `FileHelper` on older devices.

## Modified / Added files
- `app/src/main/java/com/example/inventoryapp/data/local/entities/WarehouseLocationEntity.kt` — added `qrUid: String?` field.
- `app/src/main/java/com/example/inventoryapp/data/local/database/AppDatabase.kt` — added `MIGRATION_38_39` which adds `qrUid` column and backfills deterministically using `UUID.nameUUIDFromBytes(code.toByteArray()).toString()`.
- `app/src/main/java/com/example/inventoryapp/data/local/dao/WarehouseLocationDao.kt` — added `getLocationByQrUid(qrUid: String)`.
- `app/src/main/java/com/example/inventoryapp/data/repository/WarehouseLocationRepository.kt` — insertLocation now ensures `qrUid` exists; added `getLocationByQrUid`.
- `app/src/main/java/com/example/inventoryapp/utils/MediaStoreHelper.kt` — new helper to save PNG to MediaStore and return `Uri` with fallback to `FileHelper`.
- `app/src/main/java/com/example/inventoryapp/ui/warehouse/WarehouseLocationDetailsFragment.kt` — added QR generation, preview dialog, save & share flows.
- `app/src/main/java/com/example/inventoryapp/ui/scan/ScannerFragment.kt` — new fragment: CameraX preview + ML Kit barcode scanning; resolves `qrUid` and navigates accordingly.
- Tests added under `app/src/test` and an instrumentation test skeleton under `app/src/androidTest`.

## Manual verification
1. Open the app and navigate to a location (Magazyn → wybierz lokalizację).
2. Tap `QR` button to generate and preview the QR code.
3. Save the QR (app stores `location_{qrUid}.png` in Gallery or `exports` fallback).
4. Open the in-app `Skaner` and scan the saved QR — app should navigate to the location details.
5. Scan a QR with an unknown `qrUid` — app should offer to create a new location (prefilled data).

## Running tests
- Unit tests (local):

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

- Instrumentation tests (device/emulator):

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Notes:
- Some instrumentation tests require an emulator with camera feed or a connected device.
- If migrations fail on first run, the app falls back to destructive migration (see `InventoryApplication.kt`) — backup local DB if needed.

## Implementation notes
- Backfill uses deterministic UUID derived from the existing `code` to avoid changing identifiers for the same code across installs.
- `QRCodeGenerator.generateFromString(payload)` is used to create PNG bitmaps for deep links.
- `MediaStoreHelper.saveBitmap(context, bitmap, displayName)` writes image to MediaStore (preferred) and returns `Uri` for sharing.

If you want, I can also add a short section into the main `README.md` pointing to this file.
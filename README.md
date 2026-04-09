# OK Inwentaryzacja Sprzętu

Native Android application for internal equipment inventory for company employees. Bootstrapped from the original `inventory-app`.

## Features

### ✅ Implemented
- **Barcode/QR Scanner**: Assign serial numbers to products using camera-based scanning
- **Serial Number Validation**: Automatic validation and duplicate detection
- **Room Database**: Offline-first local data storage
- **MVVM Architecture**: Clean, maintainable code structure
- **Multiple Barcode Formats**: Support for QR, EAN-13, EAN-8, Code 128, and more

### 🚧 In Progress (See PROJECT_PLAN.md)
- Equipment and assignment management
- Employee registry and relations
- Shipping/return workflows
- Data export/import

## Tech Stack

- **Language**: Kotlin
- **Minimum SDK**: API 26 (Android 8.0)
 - **Target SDK**: API 30
- **Architecture**: MVVM with Repository pattern
- **Database**: Room (SQLite)
- **Camera**: CameraX API
- **Barcode Scanning**: ML Kit
- **DI**: Manual dependency injection (Hilt planned)
- **Testing**: JUnit, Mockito, Espresso

## Getting Started

### Prerequisites
- Android Studio (Arctic Fox 2020.3.1 or newer recommended)
- Android SDK API 26–30 installed
- JDK 8 or JDK 11 installed (Gradle runs on a JDK, not a JRE)

### Build & Run
```powershell
# Windows (PowerShell)
Push-Location "C:\Users\pz_przybysz\Documents\git\ok-inv-app"
.\gradlew.bat assembleDebug
.\gradlew.bat quickDeploy
Pop-Location
```

```bash
# macOS/Linux
./gradlew assembleDebug
./gradlew quickDeploy
```

### Run Tests
```bash
# macOS/Linux
./gradlew test
./gradlew connectedAndroidTest

# Windows (PowerShell)
./gradlew.bat test
./gradlew.bat connectedAndroidTest
```

### Install on Device
```bash
# macOS/Linux
./gradlew installDebug

# Windows (PowerShell)
./gradlew.bat installDebug
```

### Troubleshooting

- Error: "Kotlin could not find the required JDK tools ... Make sure Gradle is running on a JDK, not JRE"
	- Ensure you have a JDK installed (8 or 11). A JRE alone is not sufficient.
	- Set JAVA_HOME to your JDK directory, or set `org.gradle.java.home` in `gradle.properties` to the JDK path.
	- If you use Android Studio, you can point Gradle to the embedded JDK (e.g., `C:\\Program Files\\Android\\Android Studio\\jbr`).

## Project Structure

```
inventory-app/
├── app/                    # Application module
│   ├── src/
│   │   ├── main/          # Main source set
│   │   │   ├── java/      # Kotlin source files
│   │   │   └── res/       # Resources (layouts, strings, etc.)
│   │   ├── test/          # Unit tests
│   │   └── androidTest/   # Instrumented tests
│   └── build.gradle.kts   # App-level build configuration
├── build.gradle.kts       # Project-level build configuration
├── settings.gradle.kts    # Gradle settings
├── PROJECT_PLAN.md        # Detailed project roadmap
├── IMPLEMENTATION.md      # Implementation details
└── README.md             # This file
```

## Documentation

- **[PROJECT_PLAN.md](PROJECT_PLAN.md)**: Complete project roadmap and feature list
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)**: Technical implementation details for the scanner feature

## Contributing

1. Check PROJECT_PLAN.md for uncompleted tasks
2. Create a feature branch
3. Implement the feature with tests
4. Submit a pull request
5. Update PROJECT_PLAN.md marking task as complete

## License

Internal use only.

---

## App Identifiers & Integrations

- Application ID: `com.ok.inv`
- App name: `OK Inwentaryzacja Sprzętu`
- Google Sheets integration has been removed from the codebase. The previous service and model files were deleted; re-adding this feature requires implementing a new integration.

---

## CI: Build & Release APK (for SOTI)

What this workflow does
- On push to `main` the action builds a signed Release APK and creates a GitHub Release attaching the APK(s).

Files involved
- [/.github/workflows/build-and-release-apk.yml](.github/workflows/build-and-release-apk.yml)
- [/app/build.gradle.kts](app/build.gradle.kts)

Required repository secrets (Settings → Secrets → Actions)
- `KEYSTORE_BASE64` — base64-encoded content of your keystore (.jks), single-line (no newlines).
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

How to generate `KEYSTORE_BASE64`

Windows (PowerShell):
```powershell
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes('inventory-release.keystore'))
$b64 | Out-File -Encoding ascii keystore.base64.txt
# open keystore.base64.txt and copy its content into GitHub secret KEYSTORE_BASE64
# or copy directly to clipboard (if available):
Set-Clipboard -Value $b64
```

Linux / macOS:
```bash
base64 -w0 inventory-release.keystore > keystore.base64.txt
# copy keystore.base64.txt content to GitHub secret KEYSTORE_BASE64
```

Set secrets using GitHub CLI (optional):
```bash
gh secret set KEYSTORE_BASE64 --body-file keystore.base64.txt --repo AdasRakieta/ok-inv-app
gh secret set KEYSTORE_PASSWORD --body 'YOUR_KEYSTORE_PASSWORD' --repo AdasRakieta/ok-inv-app
gh secret set KEY_ALIAS --body 'YOUR_KEY_ALIAS' --repo AdasRakieta/ok-inv-app
gh secret set KEY_PASSWORD --body 'YOUR_KEY_PASSWORD' --repo AdasRakieta/ok-inv-app
```

How to trigger and verify
- Merge or push to `main` — Actions will run and build `:app:assembleRelease`.
- Check the workflow run in the Actions tab; on success a Release `release-<sha>` is created with APK attached.
- To get a direct download link for SOTI: open the Release, right-click the APK asset Download button and "Copy link address" — use that URL in SOTI.

Notes & security
- The workflow temporarily writes `app/keystore.jks` and `app/keystore.properties` during build; do not commit the keystore.
- If your repository is private, the download link will require authentication and SOTI may not be able to fetch it. If SOTI requires public access, either make Releases public or host the APK on a public storage (e.g., signed URL on cloud storage).
- Keep your keystore and passwords private; never commit them into source control.

If you want, I can also add a small README section with the exact `gh` commands pre-filled for this repository.

## Debug: instalacja z danymi testowymi

W buildzie `debug` aplikacja pokazuje prosty ekran startowy pozwalający wybrać instalację z przykładowymi danymi testowymi — to przyspiesza testowanie funkcjonalności.

Jak użyć:

- Zbuduj i zainstaluj build `debug`:
```
./gradlew assembleDebug
./gradlew installDebug
```
- Uruchom aplikację na urządzeniu/emulatorze. W buildzie `debug` pojawi się dialog z opcją "Tak — z danymi" lub "Nie — bez danych".
- Wybierz "Tak — z danymi" aby wstępnie zasilić bazę danymi testowymi (seedery używają `EquipmentDataSeeder` i `ProductDataSeeder`).

### Demo flavor (bez pytania)

Jeśli wolisz instalować aplikację już od razu z danymi (bez dialogu), użyj przygotowanego flavora `demo`. Task `installDemo` instaluje wariant demo, który automatycznie wykonuje seed danych przy pierwszym uruchomieniu:

```powershell
.\gradlew.bat installDemo
# lub z uruchomieniem po instalacji:
.\gradlew.bat deployDemo
```

Wariant `demo` ma `applicationId` z sufiksem `.demo` (np. `com.ok.inv.demo`) i automatycznie seeduje istniejące seedery (`EquipmentDataSeeder`, `ProductDataSeeder`).

Uwaga: funkcja jest dostępna tylko w buildzie `debug` (plik `src/debug/AndroidManifest.xml` i `src/debug/java/.../DebugInstallActivity.kt`).

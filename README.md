# Inventory App

Native Android inventory management application built with Kotlin. This app helps manage products, packages, and serial numbers using barcode/QR code scanning.

## Features

### ✅ Implemented
- **Barcode/QR Scanner**: Assign serial numbers to products using camera-based scanning
- **Serial Number Validation**: Automatic validation and duplicate detection
- **Room Database**: Offline-first local data storage
- **MVVM Architecture**: Clean, maintainable code structure
- **Multiple Barcode Formats**: Support for QR, EAN-13, EAN-8, Code 128, and more

### 🚧 In Progress (See PROJECT_PLAN.md)
- Product and package management
- Shipping label generation
- Data export/import
- Multi-device synchronization

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

### Build
```bash
# macOS/Linux
./gradlew assembleDebug

# Windows (PowerShell)
./gradlew.bat assembleDebug
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

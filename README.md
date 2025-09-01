# DocScan

A modern Android document scanning app built with Jetpack Compose, Clean Architecture, and ML Kit.

## Features

- Document scanning with edge detection
- Image processing and enhancement
- PDF export functionality
- Modern Material 3 UI
- Clean Architecture with MVVM
- Dependency injection with Koin

## Architecture

The project follows Clean Architecture principles with the following modules:

- **app**: Presentation layer with Compose UI
- **data**: Data layer with repositories and implementations
- **domain**: Business logic layer with use cases and models

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Koin
- **Async**: Kotlin Coroutines + Flow
- **Image Processing**: ML Kit Text Recognition
- **Camera**: CameraX
- **Image Loading**: Coil
- **PDF Generation**: iText 7
- **Target SDK**: Android 15 (API 35)

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run

## Important Notes

### 16 KB Page Size Compatibility

This app uses ML Kit libraries which may show warnings about 16 KB page size compatibility on Android 15+ devices. This is a known issue with ML Kit libraries and doesn't affect functionality.

The build configuration includes:
- NDK ABI filters to optimize library loading
- Proguard rules for ML Kit compatibility
- Packaging options to handle native library conflicts

### Dependencies

All dependencies are managed through the version catalog in `gradle/libs.versions.toml`.

## Project Structure

```
DocScan/
├── app/                    # Presentation layer
│   ├── src/main/java/com/adhikari/docscan/
│   │   ├── di/            # Koin modules
│   │   ├── ui/            # Compose UI components
│   │   │   ├── screen/    # UI screens
│   │   │   ├── theme/     # Material 3 theme
│   │   │   └── viewmodel/ # ViewModels
│   │   ├── MainActivity.kt
│   │   └── DocScanApp.kt
├── data/                   # Data layer
│   └── src/main/java/com/adhikari/data/
│       ├── repository/    # Repository implementations
│       └── service/       # Service implementations
├── domain/                 # Domain layer
│   └── src/main/java/com/adhikari/domain/
│       ├── model/         # Domain models
│       ├── repository/    # Repository interfaces
│       ├── service/       # Service interfaces
│       └── usecase/       # Use cases
└── gradle/
    └── libs.versions.toml # Version catalog
```

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## License

This project is for educational purposes.

# Hollow Convert

Hollow Convert is a native Android file-conversion toolbox built with Kotlin and Jetpack Compose.

## Current starter features
- Native Android interface
- Android system file picker
- Text conversion/export: TXT, HTML, and JSON formatting
- GitHub Actions workflow that builds and uploads a debug APK

## Build
The GitHub Actions workflow runs on pushes to `main` and can also be started manually from the Actions tab. Download the `Hollow-Convert-debug` artifact from a successful run.

## Roadmap
- Image conversion
- Audio/video conversion through a maintained FFmpeg integration
- Archive extraction and creation
- Document and markup converters
- 3D model format conversion where compatible libraries exist

Not every format can be converted losslessly or on-device. Each format needs a compatible parser, encoder, or conversion library.

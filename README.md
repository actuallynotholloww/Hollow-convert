# Hollow Toolbox

Hollow Toolbox is a native Android file-conversion toolbox built with Kotlin and Jetpack Compose.

## Current starter features
- Native Android interface
- Android system file picker
- Text conversion/export: TXT, HTML, JSON, CSV, Base64, and URL encoding/decoding
- Image conversion: PNG, JPG, and WebP
- ZIP archive creation from multiple selected files and safe ZIP extraction into a chosen folder
- GitHub Actions workflow that builds and uploads a debug APK

## Build
The GitHub Actions workflow runs on pushes to `main` and can also be started manually from the Actions tab. Download the `Hollow-Toolbox-debug-apk` artifact from a successful run.

## Roadmap
- Image conversion
- Audio/video conversion through a maintained FFmpeg integration
- Archive extraction and creation
- Document and markup converters
- 3D model format conversion where compatible libraries exist

Not every format can be converted losslessly or on-device. Each format needs a compatible parser, encoder, or conversion library.


ZIP tools include a 64 MB input limit, a 1,000-entry limit, and a 256 MB extracted-data limit. Audio/video transcoding and specialist 3D/game-asset conversion are not included in this build yet; they require additional compatible engines.

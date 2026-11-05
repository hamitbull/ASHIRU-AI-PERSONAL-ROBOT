# ASHIRU AI — Personal Robot

An Android personal assistant prototype with a robot overlay, speech input/output, and user-controlled local memory.

## Build
Push to `main` or run **Actions → ASHIRU AI Android APK → Run workflow**. Download the `ASHIRU-AI-debug-apk` artifact.

## Current MVP
- Hausa-first speech recognition using the Android speech recognizer (device/provider support required).
- Text-to-speech (Hausa voice availability depends on installed TTS engine).
- User-controlled local memory stored on-device.
- Floating robot overlay after the user grants Android's overlay permission.

## Privacy and limitations
This prototype does not silently inspect other apps, record continuously, or send screen contents to a server. Overlay access is optional. Speech recognition may be processed by the device's configured recognition provider. Full AI conversations, accessibility-based screen assistance, and music-synced dancing are planned follow-up modules and require explicit permissions and testing.


Build workflow is configured to run on pushes to `main` and manual dispatch.

# AIMP clone — open source Android player (LRCLib lyrics)

Clean-room Android (Kotlin) music player styled after AIMP, with an **LRCLib auto-lyrics tab**.

## Features
- Library scan (MediaStore), queue, mini player, full player with seek
- Lyrics tab: auto-fetch from LRCLib (`/api/get` + `/api/search` fallback), LRC synced-line highlighting
- AIMP-style dark/orange theme
- GitHub Actions workflow builds the APK and uploads it to gofile

## LRCLib
`app/src/main/java/com/aimp/player/lyrics/` — `LrcLibApi`, `LyricsRepository`, `LyricsParser`, `LyricsViewModel`.

## Build
Android Studio or Gradle 8.7, AGP 8.5.2, Kotlin 2.0, minSdk 26.

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI
`.github/workflows/build.yml` builds on push / manual dispatch and prints the gofile link in the run summary.

## Note
This is an independent clean-room implementation, not AIMP's source. AIMP's player source is closed.

# Foreword by Charles Lane

This project has been an idea I've had for a long time, but has never quite made it off the back burner. This made it a perfect candidate to explore Claude Code as a way to develop apps faster with a somewhat low stakes project. Everything outside of this foreword has been made with Claude Code with myself acting as Project Manager.

# Easy Logger

A lightweight Android app for tracking recurring personal activities. Create custom categories, log timestamps with a single tap, and export your data to CSV — all fully offline with no account required.

## Features

- **Custom categories** — create unlimited tracking categories with full emoji support
- **One-tap logging** — instantly record the current time against any category
- **Manual entry** — log past or future timestamps with a date/time picker
- **List & grid views** — toggle between a compact list and a visual card grid
- **Paged history** — scroll through thousands of entries per category without jank
- **CSV export** — export all data via the system file picker, no permissions needed
- **Dark mode** — follows system theme with Material You dynamic colors (Android 12+)

## Examples

| Category | Use Case |
|----------|----------|
| `💧 Drank Water` | Tap "Log Now" each time you drink a glass — review your hydration history later |
| `😴 Bedtime` | Log when you go to sleep each night to spot patterns over weeks |
| `💊 Took Medication` | Quick-tap to confirm you took your meds; check the timestamp list if you can't remember |
| `💪🏽 Workout` | Use "Log Manual" to backfill a morning run you forgot to log |
| `🇯🇵 Japanese Study` | Track daily study sessions and export to CSV for a spreadsheet streak chart |

## Building

**Requirements:** Android Studio Ladybug (2024.2+), JDK 17, Android SDK 35

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest
```

The debug APK is output to `app/build/outputs/apk/debug/`.

## Architecture

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Compose Navigation (single Activity) |
| State | ViewModel + StateFlow |
| Persistence | Room (SQLite) |
| DI | Hilt |
| Paging | Paging 3 (50 items/page) |
| Export | CSV via Storage Access Framework |

**Min SDK:** 26 (Android 8.0) &bull; **Target SDK:** 35

## Project Structure

```
app/src/main/java/com/easylogger/app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   └── repository/     # Repository layer
├── di/                 # Hilt modules
├── export/             # CSV exporter
└── ui/
    ├── components/     # Shared composables
    ├── detail/         # Log detail screen
    ├── main/           # Category list screen
    ├── navigation/     # Nav routes and host
    └── theme/          # Material 3 theming
```

## License

All rights reserved.

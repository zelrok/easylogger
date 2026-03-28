# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build signed release APK (requires keystore.properties)
./gradlew test                   # Run unit tests (JVM)
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)

# Run a single test class
./gradlew test --tests "com.easylogger.app.ui.main.CategoryListViewModelTest"

# Run a single test method
./gradlew test --tests "com.easylogger.app.ui.main.CategoryListViewModelTest.testMethodName"
```

## Architecture

Single-module Android app (`com.easylogger.app`) using single-Activity + Jetpack Compose navigation.

**Layers:**

- **data/local/entity/** — Room entities: Category, LogEntry, Folder, UserPreference. Category→Folder FK (SET_NULL), LogEntry→Category FK (CASCADE). Folder has self-referential FK (parentFolderId, SET_NULL) for nested folders.
- **data/local/dao/** — Room DAOs. LogEntryDao exposes `PagingSource` for paged history. CategoryDao uses complex queries (MAX timestamp joins, count aggregations).
- **data/repository/** — @Singleton repositories wrapping DAOs. Business logic lives here (sort order calculation, timestamp formatting).
- **di/DatabaseModule.kt** — Hilt module providing AppDatabase and all DAOs at SingletonComponent scope.
- **ui/main/** — Home screen: category/folder list+grid with drag-to-reorder, folder navigation via BackHandler.
- **ui/detail/** — Log history screen: paged entries (Paging 3, 50/page), Log Now (500ms cooldown), Log Start/Stop (time windows), manual date/time entry with start+end.
- **ui/navigation/** — NavRoutes sealed class with Main and Detail(categoryId) routes.
- **export/CsvExporter.kt** — CSV export via SAF with UTF-8 BOM and ISO 8601 timestamps. Columns: category, start_time, end_time, created_at.

**Navigation:** Folder navigation uses a stack (`folderStack: List<FolderStackEntry>`) for arbitrary nesting depth. `enterFolder` pushes, `exitFolder` pops. BackHandler and TopAppBar back button both call `exitFolder()`.

**State management:** ViewModels expose `StateFlow` for UI state and `Channel` for one-shot events. Composables collect via `collectAsStateWithLifecycle()`.

**DI chain:** Hilt `@HiltAndroidApp` → `@AndroidEntryPoint` Activity → `@HiltViewModel` ViewModels (constructor-injected repositories) → `@Singleton` repositories (constructor-injected DAOs).

## Database

Room database (AppDatabase) at version 4 with exported schemas to `app/schemas/`. Migration 1→2 adds folders table and folderId/folderSortOrder to categories. Migration 2→3 renames `timestamp` to `startTime` and adds nullable `endTime` to log_entries (table rebuild). Migration 3→4 adds parentFolderId and folderSortOrder to folders for nested folder support. KSP generates Room code.

## Testing

- **Unit tests** (`app/src/test/`): JVM tests using fake DAO implementations, kotlinx-coroutines-test, and Turbine for Flow assertions. Cover repositories and ViewModels.
- **Instrumented tests** (`app/src/androidTest/`): Run on device/emulator. Test real Room database behavior for DAOs.

## Key Dependencies

Versions managed in `gradle/libs.versions.toml`. Kotlin 2.1.0, Compose BOM 2024.12.01, Room 2.6.1, Hilt 2.53.1, Paging 3.3.5. Drag-to-reorder uses `sh.calvin.reorderable:2.4.2`.

## Versioning

This project follows [Semantic Versioning 2.0.0](https://semver.org/). The `versionName` in `app/build.gradle.kts` must always be MAJOR.MINOR.PATCH:

- **MAJOR** — breaking changes (e.g. incompatible database migration, removed features)
- **MINOR** — new features (backward compatible)
- **PATCH** — bug fixes, UI polish

Android's `versionCode` is an independent integer that increments by 1 with every release, regardless of which SemVer component changes.

## Project Status

All features from the spec are implemented. The app is production-ready at v1.3.0 (versionCode 4).

**Known UI polish gaps** (minor, not blocking):

1. Haptic feedback on "Log Now" — spec calls for it, not implemented
2. Shared element transitions between list and detail screen
3. Highlight animation on newly added log entries
4. Fade-out animation on deleted items
5. Grid responsiveness on tablets (spec says 3-4 columns)
6. Grid long-press context menu as alternative to overflow icon
7. "Add" card styling — spec says dashed border in list mode, "+" card in grid

**Non-goals** (explicitly out of scope per spec): cloud sync, charts/analytics, reminders/notifications, user accounts, CSV import, color-coding, home screen widget.

## Changelog

### WIP — feature/nested-folders (branch, not yet merged)

- **Nested folders:** Folders can now contain sub-folders. Folder entity gains `parentFolderId` (self-referential FK, SET_NULL) and `folderSortOrder`. ViewModel navigation uses a folder stack for arbitrary nesting depth. Folder interior shows mixed sub-folders + categories. "Add Folder" button available at all nesting levels. Drag-drop supports folder-onto-folder. FolderListItem/FolderGridCard gain "Remove from Folder" option. Room migration v3→v4. 13 new unit tests.
- **Remaining work:** QA testing on device, version bump, merge to main.

### v1.3.0 (2026-03-28) — versionCode 4

- **Time windows:** Log entries now support start/end times. LogEntry entity: `timestamp` → `startTime` + nullable `endTime`. Three logging modes: "Log Now" (instant, startTime==endTime), "Log Start"/"Log Stop" (open/close a time window), "Log Manual" (pick start+end via 4-step date/time picker). Open entries show "in progress"; closed windows show duration. DateTimePickerDialog redesigned as 4-step flow (start date → start time → end date → end time) with "Same as start" shortcut. CSV export updated with start_time/end_time columns. Room migration v2→v3 (table rebuild). All unit and instrumented tests updated.

### v1.2.0 (2026-03-25) — versionCode 3

- **Version in title bar:** Top app bar now shows "Easy Logger v{version}" using the version from PackageInfo. Folder views still show only the folder name. (Closes #1)

### v1.1.0 (2026-03-20) — versionCode 2

- **Category folders:** Create/edit/delete folders, drag categories into folders, folder-interior reorder, "Remove from Folder" context menu, folder-aware "+" button. New Folder entity, FolderDao, FolderRepository. Room migration v1→v2 (folders table, folderId/folderSortOrder on categories).
- **Drag-to-reorder categories:** Long-press to drag in both list/grid modes. Uses `sh.calvin.reorderable` v2.4.2. Optimistic UI with Room persistence on drop.
- **Bug fix:** DateTimePickerDialog UTC/local timezone mismatch when editing existing log entries. Material 3 DatePicker operates in UTC but code was passing local-timezone millis.

## Project Documentation

- `docs/Easy_Logger_App_Spec.md` — Full product specification

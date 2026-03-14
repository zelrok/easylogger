# Product Requirements Specification
## Easy Logger — Personal Activity Tracker
### Android Application

**Version 1.0 | March 2026**

| Field | Detail |
|-------|--------|
| Platform | Android (API 26+ / Android 8.0 Oreo and above) |
| Architecture | Single-activity, Jetpack Compose UI, MVVM with Room database |
| Language | Kotlin |
| Target Devices | Phones and tablets, portrait orientation primary |
| Data Storage | Local SQLite via Room; CSV export to device storage |
| Network | None required — fully offline application |

---

## Table of Contents

- [1. Overview](#1-overview)
  - [1.1 Goals](#11-goals)
  - [1.2 Non-Goals (v1.0)](#12-non-goals-v10)
- [2. Information Architecture](#2-information-architecture)
  - [2.1 Data Model](#21-data-model)
  - [2.2 Navigation Map](#22-navigation-map)
- [3. Screen Specifications](#3-screen-specifications)
  - [3.1 Main Screen — Category List](#31-main-screen--category-list)
  - [3.2 Detail Screen — Log View](#32-detail-screen--log-view)
  - [3.3 Date/Time Picker (Manual Entry)](#33-datetime-picker-manual-entry)
- [4. CSV Export Feature](#4-csv-export-feature)
- [5. Technical Requirements](#5-technical-requirements)
  - [5.1 Architecture](#51-architecture)
  - [5.2 Permissions](#52-permissions)
  - [5.3 Performance Targets](#53-performance-targets)
  - [5.4 Database Schema (Room)](#54-database-schema-room)
- [6. Edge Cases and Validation Rules](#6-edge-cases-and-validation-rules)
- [7. UI/UX Guidelines](#7-uiux-guidelines)
- [8. Future Considerations (Post v1.0)](#8-future-considerations-post-v10)

---

## 1. Overview

Easy Logger is a lightweight Android application that lets users track recurring personal activities by logging timestamped entries against user-defined categories. The app emphasises speed, simplicity, and privacy: all data lives on-device with no account or network requirement, and can be exported to CSV at any time.

### 1.1 Goals

- Allow users to create unlimited custom tracking categories (e.g. "Bedtime", "Drank Water", "Took Medication").
- Provide one-tap logging of the current timestamp against any category.
- Support manual timestamp entry for retroactive logging.
- Display a scrollable, chronological history of all entries per category.
- Enable editing and deletion of both categories and individual log entries.
- Export all data to CSV format saved to local device storage.
- Offer a list/grid view toggle on the main screen so users can switch between a compact list and a visual grid layout.
- Support full Unicode emoji in category names, rendering them correctly across both view modes and all UI surfaces.

### 1.2 Non-Goals (v1.0)

- Cloud sync or multi-device support.
- Charts, graphs, or analytics dashboards.
- Reminders, notifications, or scheduling.
- User accounts or authentication.

---

## 2. Information Architecture

### 2.1 Data Model

The app uses two primary entities and a preferences table stored in a local Room (SQLite) database.

**Category**

| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated unique identifier |
| name | String | User-defined label, e.g. "😴 Bedtime". Stored as UTF-8; supports full Unicode including emoji. |
| sortOrder | Int | Position in the main list; supports reordering |
| createdAt | Long (epoch ms) | Timestamp of category creation |

**UserPreference**

| Field | Type | Notes |
|-------|------|-------|
| key | String (PK) | Preference identifier, e.g. "view_mode" |
| value | String | Preference value, e.g. "list" or "grid" |

**LogEntry**

| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated unique identifier |
| categoryId | Long (FK) | References Category.id; cascading delete |
| timestamp | Long (epoch ms) | The date/time being logged |
| createdAt | Long (epoch ms) | When the entry was actually created |

### 2.2 Navigation Map

The app follows a simple two-screen hierarchy:

- Main Screen (Category List) → tap category → Detail Screen (Log View)
- Main Screen → tap "Add New Item" → inline dialog or bottom sheet
- Detail Screen → "Log Now" button → instant entry
- Detail Screen → "Log Manual" button → date/time picker → entry
- Main Screen → overflow menu → "Export to CSV"
- Main Screen → top app bar toggle icon → switch between List Mode and Grid Mode

---

## 3. Screen Specifications

### 3.1 Main Screen — Category List

The main screen is the app's entry point and displays all user-created tracking categories. It supports two display modes: List and Grid.

**Layout**

| Element | Behaviour |
|---------|-----------|
| Top App Bar | Title: "Easy Logger". Leading: none. Trailing actions: a view-toggle icon (list/grid) and an overflow menu containing "Export to CSV". |
| View Toggle Icon | Toggles between list mode (default) and grid mode. Icon reflects the mode the user will switch TO (e.g. show grid icon when in list mode). Preference is persisted in the UserPreference table so it survives app restarts. |
| Category List (List Mode) | Vertical scrolling list. Each row shows the category name (with emoji rendered inline), a subtitle with the most recent log timestamp (or "No entries yet"), and trailing edit/delete icon buttons. |
| Category Grid (Grid Mode) | Responsive grid (2 columns on phones, 3–4 on tablets). Each card shows the category name centred with emoji rendered at a prominent size, plus the most recent timestamp below. Long-press or an overflow icon on each card reveals edit/delete actions. |
| Add New Item | In list mode: pinned row at the bottom styled with a dashed border or "+" icon. In grid mode: a "+" card in the last grid position. Both open a text-input dialog to name the new category. |
| Empty State | When no categories exist, display a centred illustration and prompt: "Tap + to start tracking something." Shown identically in both modes. |

**List Mode vs Grid Mode**

| Aspect | List Mode | Grid Mode |
|--------|-----------|-----------|
| Layout | Single-column vertical list | Multi-column grid (2 cols phone, 3–4 cols tablet) |
| Card content | Name + subtitle + edit/delete icons on one row | Name centred with larger emoji, timestamp below, overflow menu for actions |
| Information density | Higher — shows more items per screen | Lower — more visual, better for emoji-heavy names |
| Edit / Delete access | Inline trailing icon buttons | Overflow icon (three-dot) on each card, or long-press context menu |
| Default | Yes (first launch default) | No |
| Animation | Crossfade transition when switching modes | Crossfade transition when switching modes |

**Emoji Support in Category Names**

Category names accept and display full Unicode emoji (including multi-codepoint sequences such as skin-tone modifiers, ZWJ family sequences, and flag emoji). Emoji are rendered using the platform's native emoji font (NotoColorEmoji on most Android devices) and require no custom rendering pipeline. Examples of valid category names:

- "💧 Drank Water"
- "😴 Bedtime"
- "💪🏽 Workout" (skin-tone modifier)
- "🇯🇵 Japanese Study" (flag emoji)

**Category Row Actions**

| Action | Trigger | Behaviour |
|--------|---------|-----------|
| Open Detail | Tap anywhere on the row (except action icons) | Navigate to the Detail Screen for that category. |
| Edit | Tap pencil icon | Open a dialog pre-filled with the current name. Save updates the name in-place. |
| Delete | Tap trash icon | Show confirmation dialog: "Delete [name] and all its entries?" On confirm, cascade-delete category and all associated LogEntry rows. |

### 3.2 Detail Screen — Log View

Displayed when a user taps a category from the main screen. The screen is divided into two halves.

**Layout**

| Region | Content |
|--------|---------|
| Top App Bar | Title: category name. Back arrow to return to Main Screen. |
| Top Half (~50% of viewport) | Scrollable list of all LogEntry timestamps for this category, sorted newest-first. Each row shows the formatted date and time (e.g. "Mar 14, 2026 — 9:32 AM") with trailing edit and delete icon buttons. |
| Bottom Half (~50% of viewport) | Two equally-sized action buttons side by side: "Log Now" (left) and "Log Manual" (right). |
| Empty State | When no entries exist, the top half shows a centred message: "No entries yet. Tap a button below to log your first." |

**Timestamp List Row Actions**

| Action | Trigger | Behaviour |
|--------|---------|-----------|
| Edit | Tap pencil icon | Open the same date/time picker used by "Log Manual", pre-populated with the entry's current timestamp. Saving overwrites the timestamp. |
| Delete | Tap trash icon | Show confirmation dialog: "Delete this entry?" On confirm, remove the LogEntry row. |

**Action Buttons**

| Button | Behaviour |
|--------|-----------|
| Log Now | Immediately creates a new LogEntry with timestamp = current device time. The new entry appears at the top of the list with a brief highlight animation. A snackbar confirms: "Logged at [time]". |
| Log Manual | Opens a bottom sheet or dialog containing a date picker and a time picker, defaulting to the current date and time. User adjusts as needed, then taps "Save". A new LogEntry is created with the chosen timestamp. |

### 3.3 Date/Time Picker (Manual Entry)

Used by both "Log Manual" and the edit action on existing entries.

| Component | Specification |
|-----------|--------------|
| Date Picker | Material 3 DatePicker. Default: today (new entry) or entry's date (edit). No future-date restriction. |
| Time Picker | Material 3 TimePicker. Respects device 12h/24h setting. Default: now (new entry) or entry's time (edit). |
| Save Button | Validates that a complete date and time are selected. Creates or updates the LogEntry. |
| Cancel Button | Dismisses with no changes. |

---

## 4. CSV Export Feature

Users can export all logged data to a CSV file saved to local device storage.

**Trigger:** Overflow menu on the Main Screen → "Export to CSV".

**File Format**

| Column | Description |
|--------|-------------|
| category | The category name at time of export |
| timestamp | ISO 8601 formatted date/time of the log entry (e.g. 2026-03-14T09:32:00) |
| created_at | ISO 8601 date/time when the entry was originally created |

**File Naming:** `easylogger_export_YYYYMMDD_HHmmss.csv` (e.g. `easylogger_export_20260314_093200.csv`)

**Storage Location:** Use the Android Storage Access Framework (SAF) to let the user choose the save location via the system file picker. This avoids needing WRITE_EXTERNAL_STORAGE permissions on modern Android versions.

**Behaviour:**

1. User taps "Export to CSV" from the overflow menu.
2. System file picker opens with a suggested filename.
3. User selects a save location and confirms.
4. App writes all LogEntry rows (across all categories) to the file, sorted by category name then timestamp.
5. A snackbar confirms: "Exported [N] entries to [filename]".
6. If no entries exist, show a snackbar: "Nothing to export yet."

---

## 5. Technical Requirements

### 5.1 Architecture

| Layer | Technology |
|-------|-----------|
| UI Framework | Jetpack Compose with Material 3 |
| Navigation | Compose Navigation (single Activity) |
| State Management | ViewModel + StateFlow |
| Persistence | Room (SQLite) with coroutine-based DAO |
| Dependency Injection | Hilt |
| CSV Writing | kotlin-csv or manual StringBuilder |
| File Access | Storage Access Framework (ACTION_CREATE_DOCUMENT) |
| Emoji Rendering | Platform-native NotoColorEmoji via Compose Text; no bundled font required |
| Preferences | Room UserPreference table (view_mode persisted across sessions) |

### 5.2 Permissions

The app requires no special permissions. File export uses the Storage Access Framework, which operates via an activity result contract and does not need runtime permissions.

### 5.3 Performance Targets

| Metric | Target |
|--------|--------|
| Cold start to interactive | < 1 second |
| "Log Now" tap to confirmed entry | < 200 ms |
| Category list scroll (1,000 categories) | 60 fps, no jank |
| Timestamp list scroll (10,000 entries) | 60 fps with paging (50 entries per page via Paging 3) |
| CSV export (100,000 entries) | < 5 seconds, non-blocking with progress indicator |
| APK size | < 10 MB |

### 5.4 Database Schema (Room)

Below is the expected Room entity and DAO surface. Field names follow Kotlin conventions.

| Entity / DAO | Detail |
|-------------|--------|
| @Entity Category | Fields: id (Long, PK, autoGenerate), name (String, UTF-8 with full emoji support), sortOrder (Int), createdAt (Long) |
| @Entity LogEntry | Fields: id (Long, PK, autoGenerate), categoryId (Long, FK → Category.id, onDelete CASCADE), timestamp (Long), createdAt (Long). Index on categoryId. |
| @Entity UserPreference | Fields: key (String, PK), value (String). Stores app-level settings such as view_mode ("list" \| "grid"). |
| CategoryDao | getAll(): Flow<List<Category>>, insert(), update(), delete() |
| LogEntryDao | getByCategoryId(id): PagingSource<Int, LogEntry>, insert(), update(), delete(), getAll(): List<LogEntry> (for export) |
| UserPreferenceDao | get(key): Flow<UserPreference?>, set(pref): upsert |

---

## 6. Edge Cases and Validation Rules

| Scenario | Expected Behaviour |
|----------|-------------------|
| Duplicate category name | Allow duplicates (user may want "Water" for different contexts). No uniqueness constraint. |
| Empty category name | Disable "Save" button when the text field is blank or whitespace-only. |
| Category name length | Soft limit of 100 characters; truncate display with ellipsis if needed. |
| Rapid "Log Now" taps | Debounce at 500 ms. Show a brief cooldown indicator on the button. |
| Manual time in the future | Permit future timestamps with no warning (user may be pre-logging). |
| Delete last category | Allowed. Main screen returns to the empty state. |
| Export with no data | Show snackbar: "Nothing to export yet." Do not open the file picker. |
| Very long history list | Use Paging 3 to lazy-load in pages of 50 entries. |
| Device rotation | Compose handles recomposition; ViewModel retains state. |
| Process death | Room persists all data; ViewModel uses SavedStateHandle for transient UI state. |
| Emoji-only category name | Permitted. A name consisting solely of emoji (e.g. "💧") is valid and renders correctly in both list and grid modes. |
| Complex emoji sequences | Multi-codepoint emoji (ZWJ sequences, skin-tone modifiers, flags) are stored as-is in UTF-8. Compose Text widget delegates rendering to the platform's native emoji font; no grapheme-cluster splitting is performed by the app. |
| Emoji in CSV export | Emoji in category names are written to CSV as raw UTF-8 with a BOM header (U+FEFF) so Excel and Google Sheets render them correctly. |
| Grid mode on small screens | Minimum card width: 160dp. If screen width cannot fit two columns, fall back to single-column (effectively list mode with card styling). |
| View mode persistence | Selected mode is stored in UserPreference table and restored on app launch. Default: list mode. |

---

## 7. UI/UX Guidelines

| Guideline | Detail |
|-----------|--------|
| Design System | Material 3 (Material You) with dynamic colour theming where supported. |
| Typography | Use default Material 3 type scale. Category names: titleMedium. Timestamps: bodyMedium. |
| Colour | Support both light and dark themes. Action buttons use primary colour fill with high contrast text. |
| Iconography | Material Symbols Outlined: edit (pencil), delete (trash), add (+), export (share/download). |
| Animations | Highlight animation on newly added entries. Fade-out on deleted items. Shared element transition between list and detail. |
| Accessibility | All interactive elements have contentDescription. Minimum touch target: 48dp. Support TalkBack and Switch Access. |
| Haptics | Light haptic feedback on "Log Now" tap confirmation. |
| Emoji Rendering | Use Compose Text with no custom font override so the system NotoColorEmoji font is used. In grid mode, render the leading emoji (if present) at 32sp for visual prominence; in list mode, render inline at the same size as the category name. |
| Grid Cards | Material 3 Card (filled variant) with 12dp corner radius. Minimum height: 120dp. Centred content with 16dp internal padding. Elevation: 1dp at rest, 4dp on press. |
| View Toggle | Use Material Symbols: view_list for switching to list, grid_view for switching to grid. Animate icon swap with a 200ms crossfade. |

---

## 8. Future Considerations (Post v1.0)

The following features are explicitly out of scope for v1.0 but are anticipated for future versions:

- Data visualisation: charts showing frequency, streaks, and trends per category.
- Reminders and notifications for time-based tracking goals.
- Cloud backup and cross-device sync.
- Widget support for one-tap logging from the home screen.
- Import from CSV to restore or migrate data.
- Category grouping and colour-coding.
- Drag-to-reorder categories on the main screen.

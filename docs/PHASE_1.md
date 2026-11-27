# Phase 1 — categorized drawer

## Scope and architecture

Incremental evolution of the existing Compose/MVVM/Room launcher, not Launcher3.
Min SDK 28, target SDK 34, compile SDK 37 remain unchanged. Phases 2–8 are not
implemented by this change. No Smart Launcher source code, branding or assets used.

- Drawer: category chips, an alphabetical list preference, in-drawer search and
  real app launching. Search intersects the selected category, with a single
  Flow-based pipeline rather than racing database requests.
- Bundled local classifier: stable category IDs, exact package mappings,
  installer/platform category hints (`ApplicationInfo.category`) and keyword
  heuristics. This is not a network Play Store category lookup. Unknown apps
  remain in an explicit bucket. Classification does not use device locale.
- Long-press an app to assign/reset a category. Optional batch assignment means
  **same inferred category**, excluding Unknown and existing manual overrides.
  Drawer preferences allow adding and renaming categories. Automatic assignment
  can be disabled without deleting overrides.
- Room v2: non-destructive v1 migration adds manual override, suspended/archive
  state, icon PNG and package-update timestamp. Scans preserve usage and manual
  assignments, remove uninstalled packages, and reuse cached icons. Renamed
  categories are not overwritten by seeding. Vault storage is unchanged.
- Optional notification badges: OFF by default. In-app disclosure precedes
  Android notification-access settings. The permission-protected listener keeps
  only package/count data in memory, ignores group summaries, and clears on
  disable/disconnect. No notification message text is read/stored, no network,
  no Accessibility permission and no POST_NOTIFICATIONS runtime request.
- Suspended apps: rendered dimmed/greyscale based on Android's suspended flag.
  OEM Digital Wellbeing implementations that do not expose this flag cannot be
  detected reliably. Opening directs to system app settings; never bypasses pause.
- Archived apps: Android 15+ `MATCH_ARCHIVED_PACKAGES`/`isArchived`; dimmed cloud
  marker with restore guidance via app settings. Android 9–14 do not expose this
  archive API. No false archive detection from arbitrary uninstalled-package data.
- Wallpaper theme: Material You on Android 12+, WallpaperColors-derived tinted
  light/dark schemes on older devices, safe seed on denied color access. Changes
  use WallpaperManager callbacks; no wallpaper bitmap/storage permission.
- Drawer controls and search use resources, scrollable dialogs, system-bar/IME
  insets and flexible widths. Preferences persist across rotation/process death;
  the ViewModel retains search/selection across activity recreation.

## How to try it

1. Open launcher → Drawer. Toggle alphabetical/category mode, wallpaper colors,
   category automation, badge counts or system-state labels; refresh on demand.
2. Long-press an app → choose category → Save. Relaunch/refresh: it must persist.
3. Drawer → edit a category name or add a custom category.
4. Drawer → notification dots and counts → read disclosure → grant Android access.
   Receive/dismiss notifications, then disable badges/revoke access; counts clear.
5. Search for a shared prefix and switch category: results must satisfy both.
6. Change wallpaper, toggle system dark mode, rotate and use split-screen.

## Validation and acceptance

Automated checks added:
- JVM: category metadata/package precedence, false positives, Turkish locale,
  search/category intersection, alphabetical order, badge count/group policy,
  ViewModel selection updates.
- Android: light/dark drawer search, long-press dialog, Room v1 migration retains
  usage and category names, launcher start/resume and rotation smoke checks.
- Existing CI: lint, offline tests, both flavor builds, permission and APK size gates.

Status at implementation: CI execution pending; do not treat the phase as fully
accepted until the checks pass. Physical-device cold-start/jank/RAM benchmarks,
notification-access grant/revoke on HyperOS, actual suspended apps, Android 15
archival, live wallpaper changes and foldable/split-screen cases still require
manual/device validation. No claim of zero crashes or complete foldable testing.

Later-phase items intentionally deferred: icon packs/masks, font picker, widgets,
workspace pagination, dock/search providers, gestures, Liquid Glass, feed, separate
foldable layouts, import/backup and baseline-profile generation.

# White-theme debug audit — 1.1.1

## Theme contract

The entire app uses white/light chrome, regardless of Android dark mode or wallpaper.
The Android launch window, Compose root, Settings, home, drawer, search, feed, vault,
and Material dialogs are light. Dark text, borders, disabled states, errors and accent
buttons preserve contrast. Wallpaper colors influence **accents only**, not page or
dialog backgrounds. User photos/app icons/wallpaper samples retain their actual
image colors; optional glass uses a strong white frost to keep dark text readable.
Android's external settings screens, third-party widgets/apps and the user's actual
system wallpaper are not recolored by this app.

## Bugs found and corrected

| Finding | Fix / regression coverage |
|---|---|
| Theme followed Android dark mode, so white UI was not guaranteed | Always-light scheme; iterate wallpaper hues and assert neutral white surfaces plus WCAG 4.5:1 normal-text contrast |
| Settings/Vault transparent Columns exposed a black Android window | White platform window plus full-size Compose Surface with explicit content color; rendered-pixel assertions on actual screens |
| Black launch/navigation bars, light icons invisible after white styling | Light parent theme and dark system-bar icons; runtime assertions |
| Dark glass sample/fallback behind dark text | White fallback and white-frost overlay, rather than a black overlay |
| Independent IO jobs could reorder setting writes or overwrite state at startup | Ordered settings action worker; rapid toggle/key save/remove, whitelist and failure-recovery tests; scanning/key operations stay off the UI thread |
| Whitelist showed only the first 30 installed apps | Remove silent truncation so every installed launcher app can be selected |
| Legacy floating-pill switch looked functional but had no overlay implementation | Clearly mark unavailable; disable switch and remove the unnecessary permission-settings link |
| Older Room emissions could replace a newer optimistic home edit | Ignore obsolete revisions until the latest desired layout is observed; deterministic stale-emission tests |
| Broken layout JSON could lead to editing/saving fabricated defaults | Do not enable editing for an unreadable setup; keep saved data intact and expose backup restore; corruption test |
| New orientation could accept edits before its saved setup loaded | Clear loaded identity on setup changes; gate screen/edit writes until matching setup loads |
| singleTop HOME intents left the launcher in Settings/drawer | Explicit HOME request handling; dismiss transient home UI; actual activity regression test |
| Returning to home could repeatedly reopen a previously requested preferences dialog | Consume the navigation request once |
| Vault passphrases were displayed in clear text | Password transformation for create/confirm/unlock fields; clear input on lock/unlock; semantics assertion |
| Vault controls overlapped edge-to-edge system bars/keyboard | Apply system-bar and IME insets |
| Settings ViewModel could retain the pre-rotation Activity | Keep application context for app scanning |
| Release workflow reused v1.1.0 for subsequent main pushes | Derive version/tag from Gradle, require matching tag pushes, refuse to replace an existing release; bump to 1.1.1/versionCode 4 |

## Validation plan and boundaries

- JVM: existing classifier/gesture/search/PIN/pagination/parser tests, plus new theme,
  settings concurrency/failure and workspace stale/corrupt-state regressions.
- Android 14: existing migration/drawer/resize/glass/feed/backup/persistence tests.
  New actual-activity home → Settings → recreation → HOME → drawer → vault checks
  assert white rendered root pixels, dark system-bar icons and masked passwords.
  The HOME callback is delivered within ActivityScenario; the separate adb smoke
  test exercises real HOME-task delivery (Android will not finish a root HOME task
  at ActivityScenario teardown). Palette JVM tests use a plain Application, since
  Android Keystore startup belongs in device tests rather than palette math tests.
  The device script repeats the new test with Android dark mode enabled.
- Both APK flavors: debug/release builds and offline/network permission gates.
- Signed release: unchanged permanent key; compare identity with v1.0.1 and v1.1.0;
  upgrade-install each baseline; signed app HOME/launch/recreation with Android dark mode.

Only a successful exact CI/release run establishes a pass. This audit is not an
exhaustive security audit and does not prove the absence of all defects. No local
Android/JDK build environment is available in this sandbox. Physical Redmi/HyperOS,
foldable/external displays, third-party widget/icon-pack breadth, API 35+ archives,
real network-provider interactions and measured startup/frame/RAM profiling remain
outstanding. No signing identity, database schema or permissions are changed here.

# HyperLauncher AI 1.1.0

Kotlin/Compose implementation on the existing architecture; no Launcher3/AOSP base.
APK size and source-line limits are intentionally unrestricted. Import/file/network
bounds prevent memory abuse and ZIP bombs; they are not application-size limits.

## Implemented phases

1. **Drawer:** local automatic categorization, manual category overrides/renaming,
   alphabetical mode, category filtering and search. Optional disclosed notification
   counts, paused grayscale, Android 15 archive indicator, wallpaper-derived palette.
2. **Home:** independent orientation/display setups, proportional placement, edit
   drag and continuous resize, grid snapping/gridless placement, widget binding,
   stacks and circular page navigation. Native widget availability is Android-controlled.
3. **Dock/search:** 4–6 slots, dock pages, optional search pill, app/settings/contact
   search, local calculations/conversions, explicit browser searches, keyboard
   selection and Android drag/drop entry point. Contacts are optional and queried locally.
4. **Gestures:** home multi-touch and icon/folder assignments. Privileged actions
   require an optional, read-free Accessibility service. Existing whitelisted vault
   capture remains a separate service; neither is required for launcher use.
5. **Presentation/widgets:** sampled glass module with compatible fallback, clock,
   calendar, battery, media-session controls, local notes/photos, feed entry, and
   explicit-refresh Open-Meteo weather (network edition). Fonts/weight, separate
   clock font, disclosed Google Play font download/cache, common icon-pack appfilter
   formats, calendar prefixes, icon masks and immersive home are configurable.
6. **Feed:** RSS/Atom parsing, persistent sources/articles/read state, explicit
   one-shot refresh, no automatic subscription or polling, optional home feed entry.
7. **Adaptive layouts:** display/orientation/size/posture keys, Room persistence,
   maximum-metrics identity and separating-hinge safe pane. Not a dual-pane editor.
8. **Portability/privacy:** native ZIP export, preview/confirm restore, supported Nova
   XML geometry/folders/dock/gesture aliases, portable widget placeholders, optional
   daily local backups (last seven), PIN-hidden apps and preference label search.

## Backup contract

Native snapshots contain display/orientation layouts and their options (including
unencrypted home notes and gesture assignments), categories/manual assignments and
feed subscriptions. They are **not encrypted**. Do not share them publicly.

They omit vault data, API keys, notification content, hidden package/PIN configuration,
contacts, feed article content/read state, global DataStore appearance/drawer options,
downloaded fonts and wallpaper/photo bytes. Widget host IDs are stripped on restore.
Imported content URI grants and wallpaper versions are discarded. Choose photos again.
Nova proprietary SQLite/binary archives are rejected without changing data. Only
known textual gesture aliases are mapped; arbitrary intent actions are not imported.
KWGT user-exported attachments are included in the ZIP for manual import by KWGT;
there is no claim of access to KWGT's private app data or automatic widget rebinding.

Restore validates input before confirmation and applies Room changes in a transaction.
Matching layouts/category IDs are upserted; unrelated setups/categories remain.
Daily copies are private app files, removed by uninstall. Use external export for
long-term safety. Scheduling is opt-in through WorkManager and not exact-time.

## Privacy/network boundary

- Offline edition has no INTERNET permission; feed/weather transports cannot connect.
- Network edition: user-enabled OpenRouter AI; explicit feed refresh to chosen HTTPS
  sources; explicit weather refresh sends manually entered coordinates to Open-Meteo.
  These use separate transports; no OpenRouter key is attached to feeds or weather.
- Fonts use the Google Play Services font provider only after a disclosed download
  tap, even in the offline edition. This is another app's network capability, not a
  claim that the offline APK can itself open sockets. Choose built-in fonts to avoid it.
- Browser searches open only on an explicit result action; typing sends no query.
- Hidden apps are filtered out of drawer, home, dock, folder contents and app search.
  The PIN uses a random salt and PBKDF2-HMAC-SHA256; five failed attempts cause a
  one-minute delay. The dialog locks on stop/recreation. This is **not** system-wide
  app locking or an encrypted vault. Clearing app storage resets this configuration.

## Verification boundary

CI builds both editions, runs JVM/parser tests and Android 14 instrumentation
(drawer light/dark/search, migrations, resize, glass fallback, persistence and backup
preview validation). Release additionally checks signatures against the original
v1.0.1 identity, alignment, permissions, upgrade-install and signed app launch/HOME/
recreation. Consult the linked exact CI/release runs; this document is not a claim
that a not-yet-finished run passed.

Still requires physical Redmi/HyperOS and foldable/external-display QA, third-party
widget/provider and icon-pack breadth testing, keyboard/contact/drag interactions,
API 35+ archive behavior, full vault/AI security regression, and measured startup,
frame-time and memory profiling. Startup logs are not a substitute for profiling.

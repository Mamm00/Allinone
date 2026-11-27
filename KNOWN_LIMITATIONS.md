# Known limitations

## Build and verification

- `compileSdk 37`, `targetSdk 34`, `minSdk 28`. Current AndroidX dependencies need
  a newer compile SDK than the original target; this does not raise the target SDK.
- This sandbox has no JDK/Android SDK or access to Google/Maven/Gradle hosts.
  Compilation and emulator tests run in GitHub Actions, not locally.
- No APK-size or source-line limit. Sizes are informational. Signature, alignment,
  permission and installation/launch checks remain required for release.
- Cold startup under 500 ms, frame rate and RAM targets have **not** been proven.
  Physical Redmi/HyperOS, foldable/external-display, third-party widget and API 35+
  archive QA remain outstanding. CI emulator checks are not physical-device QA.
- Signing uses the existing permanent Actions key; missing/invalid secrets fail the
  release. v1.0.0 unsigned APKs are obsolete and cannot be installed as updates.

## Feature boundaries

- Glass samples wallpaper images chosen/applied through this launcher. Android can
  prevent reading arbitrary/live-wallpaper pixels; denied/missing samples fall back.
  Separating hinges use a safe pane; there is no full dual-pane home editor.
- Nova import supports bounded XML favorites/workspace exports, not proprietary
  binary/SQLite versions. Unknown gestures are omitted. Widgets require Android
  rebinding; KWGT exported files require manual KWGT import. See the exact backup
  inclusions/exclusions in [release notes](docs/RELEASE_1.1.0.md).
- Native backups and home notes are unencrypted. Vault contents/keys and hidden
  app/PIN configuration are excluded. Global appearance/drawer settings, downloaded
  fonts, feed article state and wallpaper/photo bytes are not transferred.
- PIN-hidden apps are launcher filtering, **not** system-wide app locks or vault
  encryption. Android Settings/other launchers still expose installed packages.
  There is no PIN recovery except clearing launcher storage (which resets settings).
- Icon packs use common assets/res-XML appfilter mappings and calendar prefixes.
  Proprietary pack engines, icon-back/mask composition, per-icon editing and all OEM
  clock animations are not supported. Unsupported mappings use cached system icons.
- Fonts are built-in families or explicit Google Play provider downloads. Provider
  availability and font weight support vary. No font downloads happen on startup.
- Weather requires manual coordinates and an explicit refresh in the network edition.
  No device location permission, automatic geolocation or background weather polling.
  Calendar needs optional READ_CALENDAR; media controls need optional notification access.
- Preferences search matches localized section labels, not every inner control/value.
  Newly added labels currently have English fallbacks; not all new controls are translated.
- The pre-existing floating-pill overlay service is not implemented. Its legacy
  settings toggle does not create an overlay; SYSTEM_ALERT_WINDOW is not declared.
- The pre-existing LLM response cache does not perform prompt-level read lookup.
  SQLCipher/native vault encryption cannot be proven by JVM-only vault unit tests;
  a full vault/AI device security regression remains necessary.

## Privacy/security

- OpenRouter certificate pins are unconfigured: normal platform TLS applies.
  Do not describe the network edition as certificate-pinned. Printing live pins in
  a CI log would not itself configure or verify an application pin policy.
- Network edition has separate explicit-purpose feed/weather transports; they do
  not use the OpenRouter API key. Feed URLs can target arbitrary chosen HTTPS hosts.
- Google Play font download is an explicitly disclosed, brokered network action,
  including in the no-INTERNET offline edition. Use built-in fonts for no such requests.
- Accessibility actions are optional and separate from the legacy text-capture
  service. Text capture remains whitelist-only and requires its own enablement.
- No telemetry/crash upload. Report failures manually. Permission/OEM restrictions
  can disable optional features; the launcher does not bypass Android restrictions.

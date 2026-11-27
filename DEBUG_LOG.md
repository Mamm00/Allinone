# DEBUG_LOG — Phase 5 bug table + self-healing log

## Phase 5 bug table

| # | Bug | Root cause | Fix | Status |
|---|---|---|---|---|
| 1 | Accessibility service never fired for whitelisted apps | `accessibility_service_config.xml` set `android:packageNames="com.hyperlauncher.ai"`, filtering events to self only | Removed `packageNames`; filtering now done in code against whitelist | Fixed |
| 2 | `SettingsRepository` whitelist compile error | Used `stringPreferencesKey` for a `Set<String>` | Switched to `stringSetPreferencesKey` | Fixed |
| 3 | Accessibility service captured itself | `if (packageName == packageName)` always true (self-comparison) | Changed to `if (packageName == this.packageName)` | Fixed |
| 4 | `BuildConfig.FLAVOR` unresolved | `buildFeatures.buildConfig` defaults off in AGP 8 | Set `buildConfig = true` | Fixed |
| 5 | Robolectric could not load `categories.json` | `isIncludeAndroidResources` defaults false | Set `testOptions.unitTests.isIncludeAndroidResources = true` | Fixed |
| 6 | Overlay permission intent failed | `ACTION_MANAGE_OVERLAY_PERMISSION` without package URI | Added `Uri.parse("package:...")` | Fixed |
| 7 | `kotlinx.serialization` unresolved in `CategorizationEngine` | Missing dependency | Added `kotlinx-serialization-json:1.9.0` to catalog + app deps | Fixed |
| 8 | Offline APK could ship INTERNET | Flavor manifest merger risk | Offline overlay explicitly `tools:node="remove"` for INTERNET; CI asserts absence via `aapt dump permissions` | Fixed + gated |
| 9 | OpenRouter cert pins unknown at scaffold time | Sandbox cannot reach `api.openrouter.ai` (DNS failure) | `LockedDownOkHttpClient` ships with empty pin list (no pinning) until CI live-handshake verification populates pins; release workflow prints SPKI pins. **Never fabricate pins.** | Open — verify in CI before release |
| 10 | SQLCipher + Room integration complexity | Room + SQLCipher `SupportSQLiteOpenHelper` wiring is fragile | Vault uses raw SQLCipher `SQLiteOpenHelper` (AES-256 at rest); main DB uses Room. Documented in architecture. | Fixed (design) |

## Self-healing log

- Phase 0: No JDK/SDK in sandbox; apt blocked (deb.debian.org unreachable).
  Decision: all compilation in GitHub Actions CI. Gradle wrapper jar fetched
  from `gradle/gradle` `v8.13.0` via GitHub API (verified PK/ZIP).
- Phase 0: Google/Maven/Gradle hosts unreachable from sandbox. SDK + deps
  download in CI only.
- Phase 0: `keytool` unavailable locally. Keystore generated in CI / by
  maintainer; base64 stored as `KEYSTORE_BASE64` secret.
- Phase 0: `api.openrouter.ai` unreachable (curl exit 6). Real SPKI pin
  obtained/verified in CI from live TLS handshake before release.
- Phase 4: CI self-healing loop — on any lint/test/build/perf failure,
  fix and re-push. No feature dropped; impossibilities documented in
  `KNOWN_LIMITATIONS.md`.

# KNOWN_LIMITATIONS

## Toolchain deviation (documented)

- **compileSdk 36 (spec said 34).** AGP 8.13.0 requires `compileSdk 36`
  (`checkAarMetadata` fails on 34). `targetSdk` remains **34** and `minSdk`
  remains **28** per spec. This is the minimal deviation to build with the
  current stable AGP.

## Hard limits (documented impossibilities)

1. **Local compilation impossible in sandbox.** No JDK, no Android SDK, and
   Google/Maven/Gradle hosts are unreachable from the sandbox. All builds run
   in GitHub Actions CI. Local `./gradlew` will fail until a JDK 21 + SDK
   are installed on a dev machine.
2. **OpenRouter certificate pins not yet populated.** The sandbox cannot reach
   `api.openrouter.ai`, so the real SPKI pin could not be fetched. The network
   flavor ships with pinning disabled (empty list) until CI verifies pins from
   the live TLS handshake. **Do not release the network APK as "pinned" until
   pins are populated and verified.** Pins are never fabricated.
3. **Release signing requires maintainer secrets.** `KEYSTORE_BASE64`,
   `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` must be created by the
   maintainer (keystore generated in CI or locally with JDK 21). Without them,
   CI produces debug-signed artifacts only.
4. **No on-device perf verification in CI.** Cold-start <350 ms, 60 fps with
   120+ apps, and idle RAM ≤80 MB require a physical Redmi 12 4G. CI enforces
   APK size + permission gates; perf gates must be validated manually on-device
   (see README setup).
5. **SQLCipher native lib on JVM tests.** `VaultHelperTest` avoids opening the
   DB (SQLCipher `.so` unavailable under Robolectric). Vault encryption is
   verified on-device, not in JVM unit tests.
6. **Floating pill is settings-only.** The overlay toggle + permission
   deep-link are implemented; the floating window service itself is not yet
   shipped (requires `SYSTEM_ALERT_WINDOW` + a foreground service). The toggle
   is inert until that service lands. `SYSTEM_ALERT_WINDOW` is not declared
   in the manifest until the pill ships.
7. **LLM response cache is write-only in v1.0.0.** `llm_response_cache` table
   exists and is cleared from Settings; prompt-level cache lookup is not yet
   wired into `NetworkOpenRouterService`.
8. **No analytics/telemetry by design.** There is no crash reporting or usage
   analytics. Crashes must be reported manually.

## Graceful degradation

- All permission denials are handled: launcher works with limited features,
  vault stays locked, AI shows offline/no-key messages, accessibility capture
  stays off.
- If Room DB is empty, the app scans installed apps on first launch.
- If `categories.json` fails to parse, all apps fall back to Others.

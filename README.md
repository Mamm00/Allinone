# HyperLauncher AI

Offline-first Android launcher for **Redmi 12 4G** (8GB / 256GB / Helio G88 / 90Hz FHD+),
HyperOS 2 / Android 14 (API 34). Min SDK 28, target SDK 34, compile SDK 37.

**Offline-first. No telemetry. No network polling. Optional daily local backups use WorkManager.**

## Visual concepts & creative ideas

Explore [four app design concepts and proposed features](docs/DESIGN_IDEAS.md),
covering the launcher home, categorized drawer, prompt-to-vault workflow, and
creative assistant. These are **AI-generated mockups, not running-app screenshots**.

| Launcher home | Categorized app drawer |
|:---:|:---:|
| <img src="docs/images/01-launcher-home.png" alt="Dark launcher home concept" width="280"> | <img src="docs/images/02-app-drawer-categories.png" alt="Categorized app drawer concept" width="280"> |

| AI prompt & encrypted vault | Creative AI assistant |
|:---:|:---:|
| <img src="docs/images/03-ai-prompt-vault.png" alt="AI prompt and encrypted vault concept" width="280"> | <img src="docs/images/04-creative-ai.png" alt="Creative AI assistant concept" width="280"> |

## Flavors (one tree, two APKs)

| Flavor | INTERNET permission | Network code | Use |
|---|---|---|---|
| `offline` (default) | **No** | Excluded at compile time | Fully offline launcher + vault + categorization |
| `network` | Yes | OpenRouter + explicit feed/weather refresh | User-initiated network features |

The offline APK is verified in CI to declare **no** `INTERNET` permission.
The network edition uses separate explicit-purpose transports for OpenRouter,
HTTPS feeds and Open-Meteo weather. No AI key is attached to feeds/weather.
OpenRouter uses normal TLS; certificate pins are **not configured**. Disclosed
Google Play font downloads are brokered by Play Services (also in the offline
edition) only when you tap Download. Built-in fonts require no request.

## White-theme debug update (1.1.1)

The app stays white/light in Android light **and** dark modes. Wallpaper colors
change accents only; home cards, Settings, vault, feed, search and dialogs retain
light surfaces. The launch window/system bars also use light styling.
See [debug audit and regression coverage](docs/DEBUG_WHITE_THEME.md).

## Features

- **Core launcher**: single-activity Compose / Material 3, MVVM + Repository,
  manual DI (no Hilt), Room for apps/categories/prompt-logs/cache.
- **Smart categorization**: 13 mandatory categories via `categories.json`
  heuristics (package match → keyword match → Others). Fully local.
- **AI prompt bar**: offline build explains AI is unavailable; network build
  calls OpenRouter only when you enable it and save your key.
- **AI Notes vault**: SQLCipher AES-256 encrypted vault with passphrase.
  Key material in EncryptedSharedPreferences (AES-256-GCM). Vault DB excluded
  from cloud backup.
- **Accessibility capture**: whitelist-only, master toggle **OFF by default**,
  disclosure before redirect to Accessibility Settings. Captures only
  whitelisted apps, only when vault is unlocked.
- **OpenRouter Creative Layer** (network flavor): user-keyed, user-enabled,
  separate client. Key never logged/committed.

- **Launcher expansion:** editable/freeform home, widget stacks, paged dock, local
  smart search, optional gestures, glass and built-in widgets, saved feed/weather,
  adaptive display setups, previewed native/Nova XML import, private daily backups,
  PIN-hidden apps, searchable preferences, fonts and icon packs.

See [1.1.0 scope and privacy notes](docs/RELEASE_1.1.0.md) for supported formats,
backup exclusions, network actions and honest verification boundaries.

## Performance targets (profiling outstanding)

- APK size is reported in CI without a hard limit (user-requested).
- Cold start < 350 ms (hard 500 ms).
- 60 fps drawer scroll with 120+ apps (LazyColumn, keyed items).
- Idle RAM ≤ 80 MB.
- Search debounce 12 ms (< 16 ms gate).
- 0 wake-locks / 0 polling.

## Build

Requires JDK 21 + Android SDK. All compilation runs in GitHub Actions CI
(sandbox has no JDK/SDK — see `DEBUG_LOG.md`).

```bash
./gradlew :app:assembleOfflineDebug
./gradlew :app:assembleNetworkDebug
./gradlew :app:testOfflineDebugUnitTest
```

## Release

`release.yml` requires all four Actions secrets: `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Signing files are ignored
by Git and removed from the runner after the job. Missing or invalid signing
credentials fail the release; there is no unsigned fallback.

Pushes to `main` and manual Release runs publish the version declared in `app/build.gradle.kts` (currently **v1.1.1**); version tags
must match that version. Already-published releases are never overwritten. Both APKs must pass signature checks against the original
v1.0.1 signing identity, alignment/permission checks, upgrade installation and
signed HOME/launch/recreation on an Android 14 emulator. This is not physical Redmi
QA or performance profiling. An APK ZIP and SHA256SUMS.txt are included.
`OPENROUTER_API_KEY` is **never** a CI secret.

The old `v1.0.0` files ending in `-unsigned.apk` are not installable. Download
`HyperLauncherAI-offline-release.apk` or `HyperLauncherAI-network-release.apk`
from a verified signed release instead. Keep the original signing key securely:
future updates must use the same key.

## Setup (Redmi 12 4G)

1. Install `HyperLauncherAI-offline-release.apk` (or `-network-` for AI/feed/weather).
2. Set as default launcher: Settings → Apps → Default apps → Home app.
3. Grant `QUERY_ALL_PACKAGES` (auto-granted for launchers; needed to list apps).
4. (Optional) Enable Accessibility capture in HyperLauncher Settings →
   disclosure → Accessibility Settings. Add whitelisted apps.
5. (Network build only) Settings → Network & AI → enable OpenRouter →
   paste `sk-or-v1-…` key → Save. Key is stored encrypted on-device only.

## Docs

- `DEBUG_LOG.md` — Phase 5 bug table + self-healing log.
- `KNOWN_LIMITATIONS.md` — known limits and impossibilities.

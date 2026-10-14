# Battery Health — Progress

Tracks implementation progress against [PLAN.md](./PLAN.md).

Legend: ✅ done · 🟡 in progress · ⬜ not started

## Milestones

| # | Milestone | Status | Notes |
|---|---|---|---|
| M1 | Project scaffold | ✅ | Gradle/Kotlin/Compose, Hilt, Room wired, CI, `BatteryInfoSource` + debug dashboard |
| M2 | Data capture | ⬜ | Room schema, snapshot worker, foreground service session tracker |
| M3 | Dashboard MVP | ⬜ | Live stats, health mapping, real-time UI via Flow |
| M4 | Estimations | ⬜ | Cycle estimator, health %, time-to-full; sysfs best-effort reader |
| M5 | History | ⬜ | Charts, session list, retention/downsampling |
| M6 | Polish | ⬜ | Alerts, dark mode, accessibility, icons, strings |
| M7 | Hardening | ⬜ | OEM quirks (Samsung, Xiaomi, Pixel, OnePlus), battery audit, Play review |

## M1 — Project scaffold (complete)

### Toolchain
- JDK 21 (Temurin 21.0.12.1) — `~/tools/jdk-21.0.12.1+1`
- Gradle 9.8.1 (+ wrapper committed)
- Android SDK — `~/android-sdk` (`platform-tools`, `platforms;android-36/37.0/37.2`, `build-tools;36.0.0/37.0.0`)
- Env shim: `~/tools/env.sh` (sourced from `~/.bashrc`)

### Versions
| Component | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 (built-in Kotlin) |
| Gradle | 9.8.1 |
| Kotlin (KGP, bundled) | 2.2.10 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Hilt | 2.60.1 |
| Coroutines | 1.11.0 |
| compileSdk / targetSdk / minSdk | 37.2 / 36 / 26 |

### Deliverables
- [x] Gradle KTS project + version catalog (`gradle/libs.versions.toml`)
- [x] Compose Material 3 theme, `MainActivity`, Hilt `@HiltAndroidApp` application
- [x] `core/model/BatterySnapshot.kt` — snapshot + status/health/plug enums
- [x] `core/battery/BatteryInfoSource.kt` — `callbackFlow` over `ACTION_BATTERY_CHANGED`
- [x] `core/battery/BatteryMappers.kt` — pure, unit-tested mappers
- [x] `di/AppModule.kt` — provides `BatteryInfoSource`
- [x] `ui/dashboard` — ViewModel + screen printing all raw values
- [x] Unit tests (`BatteryMappersTest`, 6 tests)
- [x] CI workflow (`.github/workflows/ci.yml`) — lint + test + lint artifact

### Verification
- `./gradlew lint test` → **BUILD SUCCESSFUL** (6 tests, 0 failures)
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (`app-debug.apk`, ~12 MB)

### Notable decisions / gotchas
- AGP 9 enables **built-in Kotlin**: the `org.jetbrains.kotlin.android` plugin must **not** be applied.
- Legacy KSP (`2.2.10-2.0.2`) uses `kotlin.sourceSets` and fails with built-in Kotlin → moved to independent **KSP 2.3.12**.
- AndroidX BOM requires **compileSdk ≥ 37**; SDK platforms are minor-versioned (`37.2`) → used the new DSL:
  ```kotlin
  compileSdk { version = release(37) { minorApiLevel = 2 } }
  ```

## Change log
- **Initial scaffold (M1)** — project skeleton, toolchain bootstrap, CI, battery source + tests.

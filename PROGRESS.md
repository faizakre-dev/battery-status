# Battery Health — Progress

Tracks implementation progress against [PLAN.md](./PLAN.md).

Legend: ✅ done · 🟡 in progress · ⬜ not started

## Milestones

| # | Milestone | Status | Notes |
|---|---|---|---|
| M1 | Project scaffold | ✅ | Gradle/Kotlin/Compose, Hilt, Room wired, CI, `BatteryInfoSource` + debug dashboard |
| M2 | Data capture | ✅ | Room schema, snapshot worker, foreground service session tracker |
| M3 | Dashboard MVP | ✅ | Live stats, health mapping, real-time UI via Flow |
| M4 | Estimations | ✅ | Cycle estimator, health %, time-to-full; sysfs best-effort reader |
| M5 | History | ✅ | Charts (level/temperature), session list, retention/downsampling |
| M6 | Polish | ✅ | Dark mode + dynamic color, full-charge/overheat alerts, a11y, strings, launcher icon |
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

## M2 — Data capture (complete)

### Deliverables
- [x] Room schema (v1): `battery_snapshots` + `charge_sessions` entities, enum converters, exported schemas (`app/schemas/`)
- [x] DAOs (`BatterySnapshotDao`, `ChargeSessionDao`) with Flow observe + count/trim queries
- [x] `BatteryRepository` / `BatteryRepositoryImpl` — app-facing persistence gateway
- [x] `ChargeSessionTracker` — pure session-boundary detection (opens on plug-in, closes on unplug, computes mAh added / peak temp / avg current / full-charge flag)
- [x] `ChargeTrackingService` — foreground service (dataSync type) sampling every broadcast while plugged in
- [x] `PeriodicSnapshotWorker` + `SnapshotScheduler` — 15-min background sampler via WorkManager (on-demand init, Hilt worker)
- [x] `BootCompletedReceiver` — resumes tracking after reboot if plugged in
- [x] Manifest: FGS + `dataSync` type, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, WorkManager initializer removal
- [x] Tests: `ChargeSessionTrackerTest` (6), `BatteryDatabaseTest` (Robolectric, 5)

### Verification
- `./gradlew lint test assembleDebug` → **BUILD SUCCESSFUL**
- 17 unit tests, 0 failures (`BatteryMappersTest` 6, `ChargeSessionTrackerTest` 6, `BatteryDatabaseTest` 5)

### New dependencies
`androidx.work:work-runtime-ktx:2.12.0`, `androidx.hilt:hilt-work:1.4.0` (+ compiler), `org.robolectric:robolectric:4.17`, `androidx.test:core:1.7.0`, `androidx.room:room-testing:2.8.5`

### Notable decisions / gotchas
- WorkManager uses **on-demand init**: `Application implements Configuration.Provider` and the manifest removes the default `WorkManagerInitializer` (merged via `tools:node`).
- FGS on targetSdk 36 requires a `foregroundServiceType` + matching permission → chose `dataSync` + `FOREGROUND_SERVICE_DATA_SYNC` (capped by Android 15's 6 h/day budget — acceptable for charge sessions).
- Robolectric tests pin `@Config(sdk = [34], application = Application::class)` so they don't run `BatteryStatusApp.onCreate` (which schedules WorkManager).
- Charge counter deltas are clamped ≥ 0 so a counter reset can't report negative mAh added.
- Room schema dir exported to `app/schemas/` via the KSP `room.schemaLocation` arg (committed for future migrations).

## M3 — Dashboard MVP (complete)

### Deliverables
- [x] `core/model/BatteryHealthAssessment.kt` — `HealthLevel` (GOOD/UNKNOWN/ATTENTION/POOR) + `HealthReason` + assessment result
- [x] `core/health/BatteryHealthEvaluator.kt` — pure mapping of platform health + temperature thresholds (warn 40 °C, critical 45 °C, cold ≤ 0 °C) to a coarse level; suppresses duplicate temperature reasons when health already reports overheat/cold
- [x] `core/common/BatteryFormatters.kt` — pure `Locale.US` formatters (level, voltage, current mA, temperature)
- [x] `ui/dashboard/DashboardUiState.kt` — flattened UI model + `BatterySnapshot.toDashboardUiState()` mapper
- [x] `DashboardViewModel` — exposes `StateFlow<DashboardUiState?>` derived from live snapshots (real-time via Flow)
- [x] `DashboardScreen` — battery level ring, health badge + reasons, temperature/voltage/current/technology stat cards, last-updated time, `@Preview`
- [x] Tests: `BatteryHealthEvaluatorTest` (9), `BatteryFormattersTest` (4)

### Verification
- `./gradlew lint test assembleDebug` → **BUILD SUCCESSFUL**
- 30 unit tests, 0 failures (mappers 6, tracker 6, DB 5, health evaluator 9, formatters 4)

### Notable decisions / gotchas
- Temperature is a first-class health signal: a `GOOD` platform reading is downgraded to `ATTENTION`/`POOR` at the thresholds above, so overheating shows up even when the OEM never sets `BATTERY_HEALTH_OVERHEAT`.
- `HealthLevel` ranking is GOOD < UNKNOWN < ATTENTION < POOR; each contributing signal can only *worsen* the verdict, never improve it.
- Current is shown as a signed mA value (`CURRENT_NOW`); sign convention varies by OEM, so direction is conveyed by the status label rather than assumed.
- Kept the M1 raw-value dump out of the dashboard — it belongs on a Details screen (M5/M6).

## M4 — Estimations (complete)

### Deliverables
- [x] `core/sysfs/SysfsBatteryReader` (+ `BestEffortSysfsReader`) — best-effort reads of `cycle_count`, `charge_full`, `charge_full_design`; every failure degrades to `null`
- [x] `core/model/BatteryEstimates.kt` — app-owned estimate state (design capacity, lifetime peak, accumulated cycle fraction, sysfs cycle count)
- [x] `domain/cycle/CycleEstimator` — `sessionContribution` (mAhAdded / design capacity, clamped) + `effectiveCycles` (kernel value or accumulated)
- [x] `domain/health/HealthCalculator` — health % = full/peak ÷ design × 100, clamped 0..100
- [x] `domain/health/DesignCapacityResolver` — sysfs → stored → 1.1× lifetime-peak fallback
- [x] `domain/stats/TimeToFullEstimator` — EWMA charge-counter rate; `timeToFullMillis` / `remainingMillis`; resets on charge-state flip
- [x] `domain/estimates/EstimatesReducer` — pure reducer folding sessions/snapshots/sysfs into `BatteryEstimates`
- [x] Room v2: `battery_estimates` singleton table + DAO; repository `observeEstimates` / `getEstimates` / `saveEstimates`; schema `2.json` exported
- [x] `ChargeTrackingService` + `PeriodicSnapshotWorker` update estimates on close / each run
- [x] Dashboard shows estimated **health %**, **charge cycles**, and **time to full / time left** (all `est.`-labelled when not from the kernel)
- [x] Tests: `CycleEstimatorTest` (5), `HealthCalculatorTest` (3), `DesignCapacityResolverTest` (4), `TimeToFullEstimatorTest` (8), `EstimatesReducerTest` (6), `BestEffortSysfsReaderTest` (3), plus formatter/DB additions

### Verification
- `./gradlew lint test assembleDebug` → **BUILD SUCCESSFUL**
- 65 unit tests, 0 failures

### Notable decisions / gotchas
- **Rate over current sign:** time estimation uses the *charge-counter delta rate* (EWMA), not `current_now`, because OEM sign/unit conventions are unreliable. `timeToFullMillis` requires a positive rate; `remainingMillis` a negative one.
- **Destructive migration kept:** DB is at version 2 with `fallbackToDestructiveMigration(dropAllTables = true)` (pre-release). A real `MIGRATION_1_2` should be added before any release with user data.
- **Design capacity fallback** is `1.1 × lifetime peak` (per PLAN §6.1), so a fallback health % tops out around 91% — deliberately conservative and always labelled estimated.
- `EstimatesReducer` is pure; service/worker only persist when a field other than `lastUpdatedMillis` changes (avoids a write per broadcast).
- sysfs reads are wrapped in `try/catch` for `IOException` and `SecurityException`; SELinux denials simply yield `null`.

## M5 — History (complete) + launcher icon

### Deliverables
- [x] `ui/AppRoot.kt` — bottom-navigation root (Dashboard | History); `MainActivity` now hosts `AppRoot`
- [x] `DashboardScreen` refactored to content-only (takes a `Modifier`; no internal `Scaffold`)
- [x] `ui/history/ChartModels.kt` — pure snapshot → `ChartSeries` transforms (fixed 0–100 level range; auto-scaled temperature; downsample to ≤180 points by bucket averaging)
- [x] `ui/history/LineChart.kt` — dependency-free Compose `Canvas` line chart (gridlines + area fill + line; single-point and empty handled)
- [x] `ui/history/HistoryUiState.kt` — `HistoryRange` (24h/7d) + `SessionUi` + pure `from(...)` mapper
- [x] `ui/history/HistoryViewModel.kt` — `range.flatMapLatest` over snapshots + sessions, combined into a `StateFlow`
- [x] `ui/history/HistoryScreen.kt` — range chips, level + temperature chart cards (min–max captions), session list, empty states
- [x] `domain/retention/SnapshotRetention.kt` — pure policy: 7-day raw retention, 1-hour buckets, avg/min/max level + temperature
- [x] `domain/retention/SnapshotRetentionUseCase.kt` — aggregate-then-prune, invoked from `PeriodicSnapshotWorker`
- [x] Room v3: `battery_snapshots_hourly` table + `HourlySnapshotDao`; repository `snapshotsBefore` / `observeHourlySnapshotsSince` / `saveHourlySnapshots` / `hourlySnapshotCount`; schema `3.json` exported
- [x] Adaptive launcher icon: `mipmap-anydpi-v26/ic_launcher(.xml|_round.xml)` + vector foreground/monochrome + `ic_launcher_background`; `android:icon`/`android:roundIcon` added to the manifest
- [x] Tests: `SnapshotRetentionTest` (6), `ChartModelsTest` (9), `HistoryUiStateTest` (4), `BatteryDatabaseTest` +3 (hourly round-trip/upsert, `getBefore`)

### Verification
- `./gradlew lint test assembleDebug` → **BUILD SUCCESSFUL**
- 87 unit tests, 0 failures
- `aapt2 dump badging` → `application: label='Battery Health' icon='res/mipmap-anydpi-v26/ic_launcher.xml'`

### Notable decisions / gotchas
- **Charts are Canvas-based, not Vico.** PLAN named Vico, but our Compose BOM (2026.09.00) is far newer than the latest Vico (2.1.3); a hand-rolled `Canvas` chart removes that compatibility risk, shrinks the APK, and is unit-testable via the pure `ChartModels` layer.
- **App name was never missing** — `android:label="@string/app_name"` was already set; the real gap was the absent icon (launchers showed a blank/generic tile). Fixed with an adaptive icon.
- **Downsampling loses the very first timestamp:** each bucket is represented by its *last* sample (newest) so the right edge of the chart is exact; the left edge lands one bucket in. Acceptable for 180-point smoothing.
- **Retention is idempotent and best-effort:** it reads stale raw rows, writes hourly buckets (`REPLACE` by bucket start), then deletes the raw rows. Buckets are keyed by hour start so re-runs overwrite rather than duplicate.
- **History range boundaries are captured when the range changes** (not on a timer), so the "since" window updates on tab/range interaction rather than continuously.
- Launcher icon uses an adaptive vector (minSdk 26 ⇒ `mipmap-anydpi-v26` is universally resolvable; no density PNGs needed). To swap in custom artwork: Android Studio → `res` → New → Image Asset → Launcher Icons, then replace the foreground layer.

## M6 — Polish (complete)

### Deliverables
- [x] **Dark mode**: `ui/theme/Color.kt` brand light/dark Material 3 schemes + dynamic color (Android 12+) in `BatteryStatusTheme`; `values-night/themes.xml` + night `window_background` so the window has no light flash in dark mode
- [x] **Dark-aware status colors**: `StatusPalette` (`statusPalette()`) replaces hard-coded green/amber/red/grey; used by the dashboard ring/health badge and the history temperature chart
- [x] **Alerts**: pure `domain/alerts/BatteryAlertDecider` (overheat with 45 °C/42 °C hysteresis; full-charge once per plugged-in episode) + `notifications/AlertNotifier` (dedicated `battery_alerts` channel, resumes the app on tap); wired into `ChargeTrackingService`
- [x] **Runtime permission**: `POST_NOTIFICATIONS` requested from `AppRoot` on Android 13+
- [x] **Accessibility**: level ring exposes one merged description ("Battery level 72%, Charging"); stat cards merge label+value; charts expose a min/max summary description; nav icons labelled; decorative inner ring content cleared
- [x] **Strings**: every user-visible string moved to `res/values/strings.xml` (nav, dashboard, health/status/reason labels, history, alerts, a11y); `HistoryRange` now carries a `@StringRes` label
- [x] Tests: `BatteryAlertDeciderTest` (8)

### Verification
- `./gradlew lint test assembleDebug` → **BUILD SUCCESSFUL**
- 95 unit tests, 0 failures
- `lint` clean (no errors)

### Notable decisions / gotchas
- **Alerts live in the foreground service**, evaluated per snapshot while charging. A stateless WorkManager run can't safely de-duplicate episodes, so overheat-while-discharging (screen off) is out of v1 scope.
- **Hysteresis** (fire ≥45 °C, re-arm ≤42 °C) prevents alert flapping around a single threshold; full-charge re-arms on unplug.
- **`AlertNotifier` never throws**: it checks `areNotificationsEnabled()` and wraps `notify` in `runCatching`, so a missing permission can't kill session tracking. `@SuppressLint("MissingPermission")` documents the guarded call.
- **Accessibility uses `clearAndSetSemantics`** on the ring's inner text so TalkBack reads the composed summary once instead of the raw "72%" / "Charging" fragments.
- **Charts stay theme-aware** (`primary` / `tertiary`) rather than a fixed orange, so they adapt to dark mode and dynamic color.
- Manual validation on 2 physical devices (milestone DoD) is still pending; covered by unit tests + lint only so far.

## Change log
- **M6 polish** — dark mode + dynamic color, alerts (full charge / overheat), accessibility semantics, string externalization, runtime notification permission.
- **M5 history + launcher icon** — history screen, dependency-free Canvas charts, session list, bottom-nav root, hourly retention/downsampling (Room v3), adaptive launcher icon, tests.
- **M4 estimations** — sysfs reader, cycle/health/time estimators, `battery_estimates` table (Room v2), dashboard estimates, tests.
- **M3 dashboard MVP** — health evaluator + formatters, `DashboardUiState`, ring/badge/stats UI, tests.
- **M2 data capture** — Room schema, repository, session tracker, FGS + worker, tests.
- **Initial scaffold (M1)** — project skeleton, toolchain bootstrap, CI, battery source + tests.

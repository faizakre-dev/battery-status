# Battery Health

A lightweight, **fully offline** Android app that shows your device's battery health, live charge/discharge stats, estimated charge cycles, temperature, voltage, and historical trends — using only public Android APIs (no root).

> Pre-release / testing build. See [Install & test](#-install--test) below.

---

## Features

### Dashboard
- **Battery level ring** with charging status (charging / discharging / full / not charging)
- **Health verdict** — Good / Attention / Poor, with the reasons behind it (platform health, temperature thresholds)
- **Estimated battery health %** (remaining vs. design capacity)
- **Estimated charge cycles**, from the kernel if readable, otherwise computed by the app over time
- **Time to full / time remaining** estimates while charging
- Temperature, voltage, current (signed mA), and battery technology

### History
- **24-hour and 7-day charts** for level and temperature (dependency-free Compose `Canvas` charts)
- **Charge session log** — each session's duration, mAh added, and peak temperature

### Polish
- Material 3 with **light/dark themes** and **dynamic color** (Android 12+)
- **Alerts**: full-charge and overheat notifications (with hysteresis to avoid flapping)
- **Accessibility**: merged/described semantics for screen readers
- Adaptive launcher icon

### Estimation, done honestly
Android does not expose design capacity or cycle count through public APIs, and most stock kernels block app reads of the relevant `sysfs` files via SELinux. Battery Health therefore:

1. Tries known `sysfs` paths best-effort (and probes per-OEM paths like `battery`, `bms`, `maxfg`).
2. Falls back to **clearly-labelled estimates** built from the charge sessions it logs over time.
3. Normalizes OEM quirks at runtime — e.g. `current_now` **sign is reconciled against the reported charging status**, and implausible magnitudes are discarded.

**No network, no tracking, no root.** Everything stays on your device.

---

## Requirements

- Android **8.0 (API 26)** or newer
- For the overheat / full-charge alerts: notification permission (Android 13+ asks at runtime)

---

## 📲 Install & test

The easiest way is to install the latest prebuilt APK.

### 1. Download the APK

**[⬇ Download latest release (v0.3.0)](https://github.com/faizakre-dev/battery-status/releases/latest/download/app-debug.apk)**

Or open the [Releases page](https://github.com/faizakre-dev/battery-status/releases) and grab `app-debug.apk` from the newest release.

### 2. Allow installs from your browser / file manager

When you tap the APK, Android will ask you to allow **"Install unknown apps"** for whichever app is opening it (your browser or file manager). Enable that for the one app, then continue.

### 3. Install

Tap the downloaded `app-debug.apk` → **Install** → open **Battery Health**.

> **Upgrading from an earlier build?** Uninstall the previous version first. Releases are signed with an ephemeral debug key, so installing over a differently-signed build can fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Your logged data is local to the app and will be cleared on uninstall.

### Alternative: install with adb

If you have `adb` and USB/wireless debugging set up:

```bash
adb install -r app-debug.apk
# or, if replacing a build with a different signature:
adb uninstall com.faizakram.batterystatus
adb install app-debug.apk
```

### Give it a few charge cycles

Cycle/health **estimates improve the longer the app runs** — it needs a few charge sessions to build up accurate numbers. Values that come from the kernel are shown as real values; everything computed by the app is labelled as an estimate.

---

## Build from source

### Prerequisites
- **JDK 21**
- **Android SDK** with platform **android-37** (minor 37.2) and build-tools **37.0.0**, plus `platform-tools`
- `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) pointing at your SDK

### Commands

```bash
# Lint + unit tests + debug APK
./gradlew lint test assembleDebug

# Output
# app/build/outputs/apk/debug/app-debug.apk
```

Install the locally built APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean (UI → Domain → Data) |
| DI | Hilt |
| Async | Coroutines + Flow |
| Persistence | Room (`battery_snapshots`, `charge_sessions`, `battery_estimates`, `battery_snapshots_hourly`) |
| Background | Foreground service (charge tracking) + WorkManager (periodic sampling) |
| Charts | Hand-rolled Compose `Canvas` (no third-party chart dependency) |
| Tests | JUnit + Robolectric + Turbine |

| Tool | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 (built-in Kotlin) |
| Gradle | 9.8.1 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk / minSdk | 37.2 / 36 / 26 |

---

## How it works

- A **foreground service** samples battery broadcasts **only while charging**, opening/closing a `ChargeSession` on plug/unplug.
- A **WorkManager** job samples every 15 minutes in the background while idle.
- Raw snapshots are kept for 7 days, then **downsampled to hourly buckets** to keep history compact.
- Pure, unit-tested domain classes compute cycles, health %, and time estimates; the OEM quirks layer normalizes per-device current readings.

### Permissions

| Permission | Why |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Keep tracking a charge session while the screen is off |
| `POST_NOTIFICATIONS` | Full-charge / overheat alerts and the service notification (requested at runtime) |
| `RECEIVE_BOOT_COMPLETED` | Resume tracking if the device reboots while plugged in |

No internet permission is requested.

---

## Project structure

```
app/src/main/java/com/faizakram/batterystatus/
├─ core/       # model, battery source, sysfs reader, quirks, formatters
├─ data/       # Room entities/DAOs/database, repository
├─ domain/     # cycle, health, stats, alerts, retention (pure Kotlin)
├─ service/    # ChargeTrackingService (foreground)
├─ sync/       # PeriodicSnapshotWorker, SnapshotScheduler
├─ receiver/   # BootCompletedReceiver
├─ notifications/
├─ di/         # Hilt modules
└─ ui/         # dashboard, history, theme, AppRoot
```

See [`PLAN.md`](./PLAN.md) for the full scope and [`PROGRESS.md`](./PROGRESS.md) for per-milestone status.

---

## Releases

Tagging a commit as `v*` triggers a GitHub Actions workflow that runs tests and publishes a debug APK to the [Releases page](https://github.com/faizakre-dev/battery-status/releases).

> The published APK is a **debug build** intended for direct sideloading/testing. A signed release build (AAB) is needed before any Play Store distribution.

---

## Status / roadmap

| Milestone | Status |
|---|---|
| M1 Project scaffold | ✅ |
| M2 Data capture | ✅ |
| M3 Dashboard MVP | ✅ |
| M4 Estimations | ✅ |
| M5 History + launcher icon | ✅ |
| M6 Polish (dark mode, alerts, a11y) | ✅ |
| M7 Hardening (OEM quirks, audit) | ✅ |

Planned for v2: home-screen widget (Glance), CSV export, and configurable alert thresholds.

---

## Feedback

Testing a device that shows odd values (wrong current sign, missing estimates, alerts not firing)? Please [open an issue](https://github.com/faizakre-dev/battery-status/issues) with your **manufacturer + model + Android version** — OEM quirks are the main thing this project is hardening against.

---

## License

No license has been specified for this project yet.

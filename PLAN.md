# Battery Health — Android App Plan

## 1. Goal

A lightweight Android app that shows the device's **battery health status, charge/discharge stats, estimated charge cycles, temperature, voltage, and historical trends** — using only publicly available Android APIs (no root).

---

## 2. Feature Scope

### v1 (MVP)
| Feature | Data source |
|---|---|
| Battery percentage, status (charging/discharging/full/not charging) | `BatteryManager` |
| Health state (good / overheat / dead / over voltage / failure / cold) | `BatteryManager` |
| Voltage, temperature, technology | `BatteryManager` / `BatteryManager.ACTION_BATTERY_CHANGED` |
| Charge counter (remaining µAh), current now/avg (µA) | `BatteryManager` properties |
| Estimated **battery health %** (remaining vs. design capacity, when exposed by kernel) | sysfs `charge_full` / `charge_full_design` (best-effort) |
| Estimated **charge cycles** | Algorithm over logged full-charge events (see §6) |
| Live charging stats (time to full / remaining time estimate) | computed from current + charge delta |
| 24h / 7d history charts (level, temperature) | Room + local sampling |
| Charging/discharging session log with added mAh | Room |
| Dark mode, Material 3 | Jetpack Compose |

### v2 (later)
- Notifications: full charge, overheat warning, charge limit (e.g. stop at 80%)
- Widget (Glance) showing level + health
- Export CSV of history
- Multiple device profile comparison

### Out of scope
- Root-only data (`/sys/class/power_supply/...` direct reads) — attempted only as optional best-effort with graceful fallback
- Charging control / kernel limits (requires root or OEM API)

---

## 3. Technical Constraints (read first)

1. **Design capacity & cycle count are not public APIs.** Google does not expose them. Most stock kernels don't allow app access to `/sys/class/power_supply/battery/cycle_count` due to SELinux.
   - Strategy: try known sysfs paths best-effort; if inaccessible, show **estimated** values computed by our own logging (§6) and label them clearly as estimates.
2. **Sampling while screen is off** requires a foreground service with battery-optimized behavior.
3. **Doze mode** limits background work → use `WorkManager` for periodic low-frequency syncs and a foreground service only during active charging sessions.
4. Some values (current_now sign, units) differ per OEM — normalize with a per-device quirks layer.

---

## 4. Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean (UI → Domain → Data) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Persistence | Room (snapshots, sessions, events) |
| Background | Foreground service (session tracking) + WorkManager (periodic sync) |
| Charts | Vico (Compose-native) |
| Testing | JUnit5 + Turbine + Robolectric; Compose UI tests |
| Min SDK 26 / Target SDK latest | — |

---

## 5. Project Structure

```
app/
├─ core/
│  ├─ model/          # BatterySnapshot, ChargeSession, HealthEstimate
│  ├─ battery/        # BatteryInfoSource (BatteryManager wrapper)
│  ├─ sysfs/          # BestEffortSysfsReader (cycle_count, charge_full*)
│  └─ common/         # dispatchers, time, format
├─ data/
│  ├─ local/          # Room DAOs, entities, migrations
│  ├─ repo/           # BatteryRepository, SessionRepository
│  └─ worker/         # PeriodicSyncWorker
├─ domain/
│  ├─ cycle/          # CycleEstimator
│  ├─ health/         # HealthCalculator (%, status mapping)
│  └─ stats/          # SessionStats, TimeToFullEstimator
├─ service/
│  └─ ChargeTrackingService.kt   # FGS while charging
└─ ui/
   ├─ dashboard/      # Home: big ring, health card, live stats
   ├─ history/        # Charts + session list
   ├─ details/        # Raw values, sysfs availability, estimates explainer
   └─ settings/       # Sampling interval, notifications, theme
```

---

## 6. Core Algorithms

### 6.1 Charge cycle estimation (no kernel data)
A cycle = cumulative **100% worth of charged energy**, not one plug-in event.

```
cycleEquivalent += (mAhAddedThisSession / designCapacity)  when discharging completes
```

- `designCapacity` := sysfs `charge_full_design` if readable, else fallback:
  - use the max observed `charge counter` over the app's lifetime ×1.1, or
  - use a per-model lookup from a bundled device database.
- Persist `accumulatedCycleFraction` in Room; round for display (`floor()` for whole cycles + fraction as `%`).
- Display as: **"≈ N cycles (estimated by this app)"** until real `cycle_count` becomes readable.

### 6.2 Battery health %
```
health% = charge_full / charge_full_design * 100     (if sysfs readable)
health% = lifetimePeakCharge / designCapacity * 100  (estimate fallback)
```

### 6.3 Time to full / remaining runtime
```
timeToFull = (fullChargeUah - currentUah) / chargeCurrentUa   (only while charging, current < 0 by OEM sign convention)
```
Smooth with EWMA over last 5 samples; invalidate on unplug.

### 6.4 Charge session detection
Trigger: `ACTION_BATTERY_CHANGED` status flips `STATUS_CHARGING/FULL` ↔ `DISCHARGING`.
Open a `ChargeSession` row on plug-in, close on unplug; sum `charge_counter` deltas (clamped ≥ 0) for `mAhAdded`.

---

## 7. Data Model (Room)

```
battery_snapshots(id, ts, percent, status, health, voltage_mv, temp_deci_c,
                  charge_uah, current_ua, power_mw)
charge_sessions(id, start_ts, end_ts, percent_from, percent_to,
                mah_added, max_temp_deci_c, avg_current_ua, is_full)
estimates(pk, design_capacity_uah, lifetime_peak_uah,
          accumulated_cycle_fraction, sysfs_cycle_count, last_updated)
events(id, ts, type[OVERHEAT|FULL|PLUGGED|UNPLugged], payload)
```

Retention: raw snapshots 7 days (downsample to hourly beyond), sessions forever.

Sampling cadence:
- Screen on: every 60 s
- Charging, screen off (FGS): every 60 s
- Idle/discharging: every 15 min via WorkManager

---

## 8. Permissions & Manifest

- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (or `specialUse` per policy) — charge tracking service
- `POST_NOTIFICATIONS` — service notification + alerts (runtime request)
- `RECEIVE_BOOT_COMPLETED` — resume tracking after reboot
- `WAKE_LOCK` — brief partial locks during sampling
- No internet permission needed for v1 (fully offline) → simpler Play policy review

Play Console declaration required if using FGS type `specialUse`; prefer `dataSync`-adjacent type or the `health` FGS type on API 34+.

---

## 9. Screens

1. **Dashboard**
   - Circular level ring, health badge (Good/Attention/Poor), estimated cycles, temperature, voltage, current (mA, charge/discharge), time-to-full/remaining
2. **History**
   - Line chart: level + temperature over 24h/7d
   - Session list: each charge with mAh added, duration, max temp
3. **Details**
   - All raw `BatteryManager` fields
   - Sysfs availability report (which values are real vs estimated)
   - Explainer: how cycles/health are computed
4. **Settings** — sampling interval, alerts thresholds, theme, data clear/export

---

## 10. Milestones

| # | Milestone | Deliverables |
|---|---|---|
| M1 | Project scaffold | Gradle/Kotlin/Compose setup, Hilt, CI (lint + unit tests) |
| M2 | Data capture | `BatteryInfoSource`, Room schema, snapshot worker, FGS session tracker |
| M3 | Dashboard MVP | Live stats, health mapping, real-time UI via Flow |
| M4 | Estimations | Cycle estimator, health %, time-to-full; sysfs best-effort reader |
| M5 | History | Charts, session list, retention/downsampling |
| M6 | Polish | Alerts, dark mode, accessibility, icons, strings |
| M7 | Hardening | OEM quirks testing (Samsung, Xiaomi, Pixel, OnePlus), battery-use audit, Play pre-launch review |

Definition of done per milestone: unit tests green, `lint` clean, manual test on 2 physical devices.

---

## 11. Testing Strategy

- **Unit**: `CycleEstimator`, `HealthCalculator`, `TimeToFullEstimator` — pure Kotlin, table-driven OEM sign-convention cases
- **Repository**: Room in-memory + Turbine on Flows
- **Service**: Robolectric for session start/stop broadcast handling
- **Manual matrix**: charging 0→100, partial charge, wireless, low-battery saver, reboot during charge, OEM skins

---

## 12. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Sysfs values unreadable on most devices | Design UI around estimates; make sysfs an enhancement, not a dependency |
| FGS restrictions on API 34+ | Tie service lifetime strictly to charging state; declare proper FGS type |
| Battery drain from sampling | 60 s max cadence, no network, measure with `BatteryManager` stats; audit before release |
| Estimations look "wrong" to users | Clear "estimated" labels + Details screen explaining the math |
| OEM unit/sign quirks | Detect sign at runtime (compare with status), per-brand normalization table |

---

## 13. First Steps (M1)

1. `gradle init` Android project (Kotlin DSL, version catalog)
2. Add Compose, Hilt, Room, Vico dependencies
3. CI workflow: `./gradlew lint test`
4. Implement `BatteryInfoSource` + a debug screen printing raw `BatteryManager` values to validate assumptions on the target device

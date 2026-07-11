# CLAUDE.md

This file gives Claude Code full context on the project. Read this before writing any code.

## Project

**Name:** Tether (working title, rename freely)
**One-liner:** An Android app that silently alerts a trusted contact if your phone is suddenly separated from a paired device, for example if it's snatched during a commute.
**Platform:** Android, native, Kotlin
**Author:** Nqobile Sibiya (nqobile-x)

## Why this exists

Built around a real commute, Rea Vaya, Gautrain, and Gaubus, daily, in Johannesburg. Phone or bag snatching during a commute is a real risk. Most personal safety apps require you to manually open the app and press a button during an incident, which isn't realistic when something happens fast. Tether removes that step. A small BLE tag (or a second cheap phone) stays paired to your primary phone. A sudden, ungraceful disconnect, not a slow fade, not a manual Bluetooth toggle, triggers a silent alert with your last known location to a trusted contact.

## Current status

The Jetpack Compose UI (Material 3) is wired to a real data layer: Room storage, native BLE scanning and GATT, FusedLocationProviderClient, and a rule-based debounce state machine feeding a direct-SMS alert pipeline. A foreground service ties BLE and alerting together. See `docs/UI_POLISH_NOTES.md` for the interaction/animation pass applied on top of the original Gemini-built screens.

## Tech stack

| Layer | Choice | Notes |
|---|---|---|
| Language | Kotlin | |
| UI | Jetpack Compose, Material 3 | Originally scaffolded by Gemini, lives in `ui/` |
| BLE | Android native `BluetoothLeScanner` + GATT APIs | No third-party BLE library unless a specific gap shows up |
| Background execution | Foreground Service | Required past Android 8 for reliable BLE scanning while backgrounded |
| Location | `FusedLocationProviderClient` (Google Play Services) | Last known location only, not continuous tracking |
| Disconnect detection | Custom rule-based state machine | RSSI trend plus `onConnectionStateChange`, see Architecture doc for the debounce logic. No ML in v1. |
| Alerting | `SmsManager` direct send | No backend required for v1. Works with zero data connection, which matters for the actual use case. |
| Local storage | Room (SQLite) | Contacts, paired device info, alert history |
| Backend (optional, later) | Spring Boot + PostgreSQL on Railway | Only needed for an alert history dashboard or multi-device sync. Out of scope for v1. |

## Repository structure

```
app/src/main/java/com/example/tether/
  ble/          - scanning, GATT connection, RSSI monitoring
  alerting/     - debounce state machine, disconnect classifier, SmsManager wrapper, alert pipeline
  data/         - Room entities, DAOs, repository, settings store
  location/     - FusedLocationProviderClient wrapper
  service/      - the foreground service tying BLE + alerting together
  ui/           - Compose screens (originally produced by Gemini, wired to real data)
  ui/theme/     - Material 3 theme, colors, type
  ui/common/    - shared motion utilities for the UI polish pass
MainActivity.kt
TetherApplication.kt
```

## Conventions

- No em dashes anywhere, in code comments, commit messages, or docs. Use a period to start a new sentence, or a comma to continue it.
- Keep the debounce/disconnect logic rule-based and explainable. Don't reach for ML or heuristics that can't be described in one sentence, this needs to be debuggable and explainable in an interview.
- Every BLE and location permission request needs a clear rationale string shown to the user before the system prompt fires.
- Favor Android's built-in APIs over third-party libraries unless there's a specific, named gap.
- Do not add a Claude/Anthropic co-author trailer to git commits for this project.

## MVP scope (v1)

- [x] Pair one BLE device (or a second phone acting as a beacon)
- [x] Foreground service monitors connection continuously
- [x] Rule-based debounce distinguishes a real disconnect event from normal signal loss (elevators, tunnels, crowding)
- [x] On confirmed disconnect, send SMS with last known GPS to one or more trusted contacts
- [x] Local alert history log
- [x] Settings screen to tune debounce sensitivity

## Explicitly out of scope for v1

- Multi-device sync or cloud backend
- ML-based anomaly detection
- The offline BLE mesh network idea (that's a separate, later project)

## Where to look next

See `docs/ARCHITECTURE.md` for the full system design, the debounce state machine, and the data model. See `docs/UI_POLISH_NOTES.md` for the interaction and animation polish pass.

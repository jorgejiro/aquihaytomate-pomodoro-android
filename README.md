# ¡Aquí hay tomate!

A Pomodoro timer for Android with a **one-cell home screen widget**. Dark, red, and out of your way.

Every Pomodoro app ships a 2×2 widget at minimum. This one fits in a single cell: **tap to
start or pause, double-tap to reset** — without ever opening the app.

> The name is a Spanish pun: *pomodoro* is Italian for tomato, and "¡Aquí hay tomate!" nods to an old
> Orlando tinned-tomato commercial.

---

## Features

### Timer
- Full Pomodoro cycles: focus, short break, and a long break every N Pomodoros.
- Every duration configurable — focus (1–180 min), short break (1–60), long break (1–120), and
  Pomodoros per cycle (2–12).
- Optional auto-start of the next slot.
- Survives the app being swiped away, the screen being off, and a device reboot. Fires on the right
  second.

### One-cell widget
- Occupies a **single home screen cell** (1×1).
- Tap to start, pause or resume. Double-tap to reset.
- Shows the remaining time and drains as the slot progresses. Focus is red, breaks are amber.
- The countdown ticks in the launcher's process, so it costs nothing in battery.

### Statistics
- Pomodoros completed and time focused, per day, week and month.
- Day streak, monthly heatmap, and a 30-day trend.
- All charts drawn by hand — no charting library, no third-party analytics.

### Alerts
- Selectable end-of-slot sound, or silence.
- **Vibration duration configurable in seconds** (5 s by default).
- Respects silent mode and Do Not Disturb.

### Everything else
- Dark theme only, on purpose.
- Spanish and English.
- No accounts, no cloud, no ads, no tracking. All data stays on the device.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, custom dark theme |
| Architecture | MVVM + UDF, `ui` / `domain` / `data` |
| DI | Hilt |
| Storage | Room (history) + DataStore Preferences (settings and timer state) |
| Timer engine | Persisted state + foreground service + `AlarmManager` backstop |
| Widget | `RemoteViews` with a native `Chronometer` |
| Charts | Compose `Canvas`, hand-rolled |
| Fonts | Inter + Space Grotesk (SIL OFL 1.1) |
| Tests | JUnit 4, MockK, Turbine, Compose UI Test |

---

## Requirements

- Android 12 (API 31) or newer.
- Android Studio with JDK 21.

## Building

```bash
./gradlew assembleDebug
./gradlew lint test
```

Release builds need a `keystore.properties` at the repo root (not committed):

```properties
storeFile=../aquihaytomate-release.jks
storePassword=…
keyAlias=aquihaytomate
keyPassword=…
```

## Documentation

- `CLAUDE.md` — project guide: stack, conventions, architecture, roadmap.
- `docs/design-spec.md` — full visual specification.
- `docs/decisions/` — architecture decision records.
- `docs/play-store-publication-texts.md` — store listing and release checklist.

## License

Personal project. All rights reserved.

Bundled fonts (Inter, Space Grotesk) are licensed under the SIL Open Font License 1.1; see
`app/src/main/res/raw/licenses_ofl.txt`.

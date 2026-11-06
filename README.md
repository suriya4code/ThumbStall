<div align="center">

<a href="https://suriya4code.github.io/ThumbStall/"><img src="assets/readme/hero.webp" width="100%" alt="ThumbStall: a 3D column of social feed cards doomscrolling around a film-reel logo, then stalling to a stop while a breathing countdown runs, next to the headline “Stall the scroll. Keep your thumb.”" /></a>

# ThumbStall

**Free, open-source anti-doomscrolling app for Android.**
<br />
*Pause the reel. Play your life!*

[![Website](https://img.shields.io/badge/Website-suriya4code.github.io%2FThumbStall-c5cc9e?style=for-the-badge&logo=githubpages&logoColor=white&labelColor=13140f)](https://suriya4code.github.io/ThumbStall/)

[![CI](https://github.com/suriya4code/ThumbStall/actions/workflows/ci.yml/badge.svg)](https://github.com/suriya4code/ThumbStall/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/suriya4code/ThumbStall?color=5a6b2e&label=release)](https://github.com/suriya4code/ThumbStall/releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-5a6b2e.svg)](LICENSE)
[![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3ddc84.svg?logo=android&logoColor=white)](#install)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7f52ff.svg?logo=kotlin&logoColor=white)](#build-from-source)
[![No INTERNET permission](https://img.shields.io/badge/INTERNET-not%20requested-c2622d.svg)](#privacy)

[**Website**](https://suriya4code.github.io/ThumbStall/) · [**Download APK**](https://github.com/suriya4code/ThumbStall/releases/latest) · [**Build from source**](#build-from-source) · [**How it works**](#how-it-works) · [**Contribute**](CONTRIBUTING.md)

</div>

---

ThumbStall adds a brief pause before opening distracting apps, giving you a moment to reconsider. No accounts, no internet, no tracking — everything stays on your device.

> **Try it in your browser:** the [website](https://suriya4code.github.io/ThumbStall/#try) has a working pause screen that runs the same escalation rule as the app.

## Features

- **Pause screen** — A breathing countdown before any app you choose
- **Escalating delays** — Repeat opens get longer pauses
- **Session budget** — Grant yourself 5/10/15 minutes when you really need it
- **Snooze** — Temporarily disable protection (15 min / 1 hour / until tonight)
- **Essentials allowlist** — Dialer, SMS, and maps are never blocked
- **Positive stats** — See time reclaimed, not shame metrics
- **OEM battery guide** — Manufacturer-specific instructions to keep protection running
- **Dark mode** — Follows system theme
- **Fully offline** — No internet permission, no analytics, no cloud

## How it works

<img src="assets/readme/pipeline.svg" width="100%" alt="Anatomy of a pause: the app notices which app opened, lets essentials through, checks your targets, snooze and budget, gates the overlay, escalates the pause, then shows the breathing screen" />

A foreground service polls `UsageStatsManager` for the app in front. Every decision after that is made by the pure Kotlin rules engine in `core/`, which has no Android dependencies and is tested on the JVM: escalation is `min(base + 5 s × repeats in the last hour, 30 s)`.

## Screenshots

<p align="center">
  <img src="design_screens/04-pause-screen.png" width="24%" alt="Pause screen with breathing countdown" />
  <img src="design_screens/01-onboarding-intensity.png" width="24%" alt="Onboarding: choose an intensity" />
  <img src="design_screens/02-onboarding-permissions.png" width="24%" alt="Onboarding: permissions explained" />
  <img src="design_screens/06-snooze-sheet.png" width="24%" alt="Snooze sheet with a costly turn-off" />
</p>

<sub>From the design set. Some screens preview roadmap features such as feed filtering.</sub>

## Install

### From GitHub Releases

1. Download the latest `app-foss-release.apk` from [Releases](https://github.com/suriya4code/ThumbStall/releases)
2. Enable "Install from unknown sources" for your browser/file manager
3. Install the APK

### Android 13+ Note

On Android 13 and newer, you may need to grant "Allow restricted settings" before Usage Access will work:
1. After installing, go to **Settings → Apps → ThumbStall**
2. Tap the ⋮ menu → **Allow restricted settings**
3. Then grant Usage Access from the app

## Build from Source

Requirements:
- Android Studio or JDK 17+
- Android SDK with platform 36

```bash
# Clone
git clone https://github.com/suriya4code/ThumbStall.git
cd ThumbStall

# Build debug APK
./gradlew assembleFossDebug

# Output: app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

For release builds, see [docs/signing.md](docs/signing.md).

## Project Structure

```
app/         # Android app (Compose UI, services, Room database)
core/        # Pure Kotlin rules engine (JVM-tested, no Android deps)
docs/        # Design specs, plans, signing guide
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GPL-3.0 — see [LICENSE](LICENSE).

## Privacy

- **No internet access** — The app has no INTERNET permission
- **No accounts** — No sign-up, no login, no cloud sync
- **No tracking** — No analytics, no crash reporting, no telemetry
- **Local only** — All data stays on your device

## Roadmap

v0.1 (current):
- [x] Pause screen with breathing countdown
- [x] App picker, escalation, session budget
- [x] Snooze and costly override
- [x] Stats dashboard
- [x] Onboarding flow

Future:
- [ ] Feed blocking (Reels, Shorts) via Accessibility
- [ ] Daily time limits
- [ ] Schedules (work hours, bedtime)
- [ ] F-Droid / IzzyOnDroid distribution

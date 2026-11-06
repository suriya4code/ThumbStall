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

<img src="assets/readme/divider.svg" width="100%" alt="" />

ThumbStall adds a brief pause before opening distracting apps, giving you a moment to reconsider. No accounts, no internet, no tracking — everything stays on your device.

<a name="try"></a>
<a href="https://suriya4code.github.io/ThumbStall/#try"><img src="assets/readme/header-try.svg" width="100%" alt="Try a pause: this is what your thumb meets first." /></a>

<a href="https://suriya4code.github.io/ThumbStall/#try"><img src="assets/readme/pause-demo.svg" width="100%" alt="The pause screen: a breathing countdown from 8 asks Still want to scroll? The 5, 10 and 15 minute choices unlock when it reaches zero. Beside it, the escalation ladder: 8, 13, 18, 23, 28 and a 30 second cap." /></a>

Most opens happen on autopilot. A short pause is enough to notice, and closing is always one tap. Open the same app again within the hour and the wait grows by 5 seconds, up to 30. **[Try the live version on the website →](https://suriya4code.github.io/ThumbStall/#try)**

<a name="features"></a>
<img src="assets/readme/header-features.svg" width="100%" alt="Features: no blocks, no locks, just a moment." />

<img src="assets/readme/features.svg" width="100%" alt="Nine features: breathing pause, escalating delays, session budget, snooze, costly override, essentials allowlist, positive stats, OEM battery guide, fully offline." />

<details>
<summary><b>Feature list as text</b></summary>

- **Pause screen** — A breathing countdown before any app you choose
- **Escalating delays** — Repeat opens get longer pauses
- **Session budget** — Grant yourself 5/10/15 minutes when you really need it
- **Snooze** — Temporarily disable protection (15 min / 1 hour / until tonight)
- **Costly override** — Turning protection off takes a reason and a 24-second wait
- **Essentials allowlist** — Dialer, SMS, and maps are never blocked
- **Positive stats** — See time reclaimed, not shame metrics
- **OEM battery guide** — Manufacturer-specific instructions to keep protection running
- **Dark mode** — Follows system theme
- **Fully offline** — No internet permission, no analytics, no cloud

</details>

<a name="how-it-works"></a>
<img src="assets/readme/header-how.svg" width="100%" alt="How it works: from a tap to a breath, in under a second." />

<img src="assets/readme/pipeline.svg" width="100%" alt="Anatomy of a pause: the app notices which app opened, lets essentials through, checks your targets, snooze and budget, gates the overlay, escalates the pause, then shows the breathing screen" />

<img src="assets/readme/stats.svg" width="100%" alt="900 ms polling interval, 30 s longest possible pause, 35 unit tests on the core, 0 network requests" />

A foreground service polls `UsageStatsManager` for the app in front. Every decision after that is made by the pure Kotlin rules engine in `core/` (`PauseRules`, `OverlayGate`, `OpenTracker`, `BudgetTracker`, `SnoozeState`, `StatsWindows`), which has no Android dependencies and is tested on the JVM.

```
app/         # Android app (Compose UI, services, Room database)
core/        # Pure Kotlin rules engine (JVM-tested, no Android deps)
docs/        # Design specs, plans, signing guide
```

<a name="privacy"></a>
<img src="assets/readme/header-privacy.svg" width="100%" alt="Privacy: the best privacy policy is a missing permission." />

<img src="assets/readme/privacy.svg" width="100%" alt="AndroidManifest permissions and why: usage stats, overlay, foreground service, notifications, boot completed and exact alarms. INTERNET is struck through: never requested." />

- **No internet access** — The app has no INTERNET permission
- **No accounts** — No sign-up, no login, no cloud sync
- **No tracking** — No analytics, no crash reporting, no telemetry
- **Local only** — All data stays on your device

<a name="screenshots"></a>
<img src="assets/readme/header-screens.svg" width="100%" alt="Screens: olive, cream and a lot of quiet." />

<img src="assets/readme/screens.webp" width="100%" alt="A 3D carousel of ThumbStall design screens turning: the pause screen, onboarding intensity, permissions explained, and the snooze sheet." />

<sub>From the design set. Some screens preview roadmap features such as feed filtering.</sub>

<a name="install"></a>
<img src="assets/readme/header-install.svg" width="100%" alt="Install: two minutes to a calmer thumb." />

### From GitHub Releases

1. Download the latest `app-foss-release.apk` from [Releases](https://github.com/suriya4code/ThumbStall/releases)
2. Enable "Install from unknown sources" for your browser/file manager
3. Install the APK

> [!NOTE]
> On **Android 13 and newer**, grant "Allow restricted settings" before Usage Access will work:
> **Settings → Apps → ThumbStall → ⋮ → Allow restricted settings**, then grant Usage Access from the app.

### Build from Source

<img src="assets/readme/terminal.svg" width="100%" alt="Terminal: git clone, cd ThumbStall, ./gradlew assembleFossDebug, BUILD SUCCESSFUL, debug APK written" />

Requirements: Android Studio or JDK 17+, and the Android SDK with platform 36.

```bash
git clone https://github.com/suriya4code/ThumbStall.git
cd ThumbStall
./gradlew assembleFossDebug   # → app/build/outputs/apk/foss/debug/app-foss-debug.apk
./gradlew :core:test          # rules engine tests
```

For release builds, see [docs/signing.md](docs/signing.md).

<a name="roadmap"></a>
<img src="assets/readme/header-roadmap.svg" width="100%" alt="Roadmap: v0.1 is out, the reel keeps turning." />

<img src="assets/readme/roadmap.svg" width="100%" alt="v0.1 shipped: pause screen with breathing countdown; app picker, escalation, session budget; snooze and costly override; stats dashboard; onboarding flow. Next: feed blocking for Reels and Shorts, daily time limits, schedules for work hours and bedtime, F-Droid and IzzyOnDroid." />

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GPL-3.0 — see [LICENSE](LICENSE).

<br />

<a href="https://suriya4code.github.io/ThumbStall/"><img src="assets/readme/footer.svg" width="100%" alt="ThumbStall. Pause the reel. Play your life!" /></a>

# ThumbStall v0.1 — Design Spec

**Date:** 2026-11-05
**Status:** Approved for planning
**Source research:** `Distraction free Android app features.md` (repo root)

## 1. Intent

ThumbStall is a free, open-source (GPL-3.0-or-later) anti-doomscrolling Android app
for casual users. It is a speed bump, not a wall: a short pause with an easy exit
whenever a distracting app opens, free for unlimited apps, with every failure mode
failing open. Built solo (+ AI agents) at nights-and-weekends cadence, so every
phase ends with an installable APK.

**Success metric:** months of active protection per user — reliability and low
annoyance over strictness.

**v0.1 is a deliberate trim** of the research doc's MVP. Feed blocking
(Reels/Shorts), daily limits, schedules, digests, grayscale, and the launcher are
all deferred. Distribution is **GitHub Releases only** — IzzyOnDroid and Google
Play are deferred by explicit decision.

## 2. v0.1 Feature Set (locked)

1. **Pause screen** on user-chosen apps: 6–10 s breathing animation, **"Close app"
   as the primary (filled) button**; "Continue" appears only after the delay.
2. **Session budget**: after Continue, pick 5 / 10 / 15 minutes; nudge at expiry,
   then the pause returns.
3. **Escalating delay**: +5 s per repeat open within a rolling 60 min, capped at 30 s.
4. **Attempts-today counter** on the pause screen.
5. **Essentials never paused** (hardcoded): phone, SMS, maps, banking,
   authenticators, camera. Overlays cleared during calls.
6. **Snooze with auto-resume**: 15 min / 1 h / until tomorrow. No "disable".
7. **Costly override**: "Open anyway" after a 30 s wait; always logged.
8. **Positive stats**: dismissals and "minutes reclaimed" framed as wins; time and
   opens per app as context. No streaks, ever.
9. **2-minute onboarding**: mechanism explainer ("you're always in control"),
   top-3 attention apps pre-checked from usage data, one permission at a time,
   Gentle / Balanced / Firm intensity presets.
10. **Protection health check**: visible "protection is OFF — tap to fix" banner,
    plus an OEM keep-alive guide (Samsung / Xiaomi / OnePlus deep links).

**Permissions (all of v0.1):** Usage Access (`PACKAGE_USAGE_STATS`), Display over
other apps (SAW), one `specialUse` foreground service. Nothing else. No INTERNET
permission, no account, no analytics.

**Balanced preset defaults** (from research doc): 8 s pause (Gentle 5 s / Firm
15 s); escalation as above; override wait 30 s (Firm: typed reason); snooze always
auto-resumes.

## 3. Architecture

**Stack:** Kotlin, Jetpack Compose + Material You, minSdk 26. Room (events) +
DataStore (settings). Build flavors `play` and `foss` from day one (manifest/flag
difference only).

**Structure — two Gradle modules:**

```
app/
  service/    MonitorService — specialUse FGS; polls UsageStatsManager.queryEvents
              ~every 900 ms while screen is ON (screen-off receiver stops polling);
              re-arms on BOOT_COMPLETED
  overlay/    PauseActivity — translucent, single-instance, excluded from recents,
              launched over the target app; hosts pause / budget / override UI
  usage/      Foreground-app detection: event parsing, debounce, last-package cache,
              backdated first query window
  ui/         Compose screens: onboarding (×3), home/stats, settings, OEM guide
  data/       Room: events (attempts, dismissals, overrides, budgets, snoozes);
              DataStore: per-app config, intensity preset
core/         Pure-Kotlin rules engine (no Android deps): given (app opened,
              history, config, now) → decision (pause Xs | within budget | budget
              expired | snoozed | essential). Monotonic time (elapsedRealtime
              injected), TIME_CHANGED-safe. Fully unit-tested.
```

`core` is a separate module so the intervention state machine is JVM-testable with
zero Android dependencies. Further module splits are deferred until the detector
library phase forces one.

**Data flow (the one loop):**

1. `MonitorService` detects a foreground package change.
2. Asks `core` for a decision (essentials short-circuit here; any internal error →
   no block — fail open).
3. Pause decision → launch `PauseActivity` with the verdict (delay length,
   attempts-today, escalation level). De-duplicate launches (skip if already
   showing; same package within 1 s).
4. User chooses: **Close app** → HOME intent, log dismissal · **Continue** → pick
   budget, log · after 30 s wait, **Open anyway** → log override.
5. Budget expiry → nudge notification → next detection re-pauses.
6. Every event lands in Room → stats screen reads aggregates.

**Error-handling principle (non-negotiable):** every failure fails open. Service
killed → health banner, never a stuck overlay. Overlay can't launch → the app
opens normally. Call in progress → overlay cleared. Uncertainty never blocks.

## 4. Phase Plan

Each phase exits with a working, installable APK.

| Phase | Scope | Exit criteria |
|---|---|---|
| **0 — Walking skeleton** (~1–2 weekends) | Scaffold (app + core, flavors, GitHub Actions CI), permission request screens, FGS detecting foreground changes, crude overlay over one hardcoded app with a working "Close" | Open Instagram on a real phone → ThumbStall covers it → Close works. Detection latency, overlay-over-app, and OEM survival proven first |
| **1 — The pause, done properly** (~2–3 weekends) | `core` rules engine via TDD (pause, escalation, essentials, monotonic time); real Compose pause screen (breathing, delayed Continue, attempts counter); app picker; events to Room | Features 1, 3, 4, 5 on any chosen app; `core` tests green |
| **2 — Budgets, snooze, override** (~2 weekends) | Budget flow (pick → expiry nudge → re-pause), snooze with auto-resume, 30 s-wait override, call-clears-overlay | Features 2, 6, 7; the full intervention loop is complete |
| **3 — Onboarding + stats + health** (~2–3 weekends) | Onboarding (explainer, top-3 preset, one-permission-at-a-time, intensity presets); stats screen; health banner + OEM guide | All 10 features done; a stranger can install and self-serve |
| **4 — First public release v0.1** (~1–2 weekends) | Polish pass (design skills), TalkBack labels, dark mode, LICENSE, README + screenshots, basic CONTRIBUTING.md | Tag → GitHub Release with signed APK, installable by the public |

**Deferred (each gets its own design later):** feed blocking (Accessibility +
detector module + fixture harness — the big one), daily limits / app groups,
schedules, digests, grayscale, launcher, localization, F-Droid / IzzyOnDroid /
Play distribution.

## 5. Testing

- **`core` — the only heavily tested thing.** Plain JUnit on the JVM. Every
  decision path: escalation math, budget expiry, snooze auto-resume, essentials,
  clock-change defense. TDD here; a bug here means a user locked out or silently
  unprotected.
- **Detection & overlay — manual smoke checklist** (`TESTING.md`), run on a real
  device before each tag: pause appears < 1.5 s; Close works; Continue → budget →
  re-pause; survives screen off/on; survives reboot; phone call clears overlay.
  Instrumented UI tests deliberately skipped (expensive, flaky).
- **Room migration test** from schema v1 onward — "never wipe user data".
- **CI:** build both flavors + run `core` tests on every push. Nothing more.

## 6. Release Discipline

- Tag `v0.x.y` → Actions builds a signed APK → GitHub Release with changelog.
- One signing key, generated at Phase 0, kept offline. Key continuity matters for
  any future store.
- `main` stays shippable; short-lived branches, squash-merge.
- Sideload note: Android 13+ users must tap "Allow restricted settings" in App
  info before granting Usage Access — illustrated note in README and onboarding.



## 7. Visual Design

Canonical screen designs live in `design_screens/` (source PDF + per-screen
PNGs). The palette is warm cream surfaces with deep-olive primary and a
near-black olive pause screen; rounded cards; calm, non-shaming copy.
Implement with Material 3 components themed to this palette (dynamic color
can override hues on 12+; the design's tone and layout are what's pinned).

**Screen → phase map (designs cover more than v0.1 — build only the
current phase's slice):**

| Design | Screen | Phase |
|---|---|---|
| `01-onboarding-intensity.png` | Intensity picker — **Gentle / Balanced (recommended) / Focused** cards + "why a few seconds works" note | Phase 3 (supersedes the "Firm" naming in §2) |
| `02-onboarding-permissions.png` | "What ThumbStall needs, and why" — permission cards with Needed/Optional/Recommended chips; trust footer "No internet access · No account · Open source" | Phase 3 (Accessibility card deferred to feed-blocking phase; keep-alive card is the OEM guide) |
| `03-today-dashboard.png` | Today tab — "Protection is on · N apps guarded" pill, **Time reclaimed** hero stat, "you closed X of Y openings", per-app list | Phase 3 (short-video allowance bar + Rules/Review tabs are later phases; v0.1 ships Today + Settings only) |
| `04-pause-screen.png` | Pause — breathing countdown circle ("6 · breathe in"), "Still want to scroll?", **Close [app]** as filled primary, 5/10/15 min chips revealed after delay ("Open for · available in 6s"), "9th open today" + "you closed it instead 23 times this week" | Phases 1–2 (canonical pause design) |
| `05-feed-filter-rules.png` | Feed filter rules with fail-open promise copy | Deferred — feed-blocking phase |
| `06-snooze-sheet.png` | Snooze — 15 min / 1 h / Until tonight each showing "Back on at <time>"; turn-off requires typed reason + "Turn off in 0:24" countdown | Phase 2 (turn-off-with-reason replaces a bare stop toggle) |
| `07-weekly-review.png` | Weekly review — reclaimed-time bar chart, closed-instead count, days-within-allowance, gentle escalation suggestion | Deferred — v1 weekly review |

**Copy pinned by the designs (v0.1 scope):** "Still want to scroll?";
"Close [app]" primary; "Open for · available in Ns"; "Nth open today";
"You closed it instead N times this week"; "Time reclaimed"; "Protection
is on · N apps guarded"; snooze options show their auto-resume time
("Back on at 1:40 PM"); intensity preset names **Gentle / Balanced /
Focused**.

## 8. Out of Scope for v0.1 (explicit)

No Accessibility service, no notification listener, no Shizuku, no VPN, no
launcher, no streaks, no account, no network, no analytics, no donation links in
any build yet, no localization. All features free, forever — no paid tier will
ever exist.

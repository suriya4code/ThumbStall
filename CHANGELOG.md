# Changelog

All notable changes to ThumbStall will be documented in this file.

## [0.1.0] - 2026-11-05

### Added

**Core Features**
- Pause screen with breathing countdown animation
- App picker to choose which apps to pause
- Escalating delays — repeat opens within an hour get longer pauses
- Session budget — 5/10/15 minute chips to grant temporary access
- Snooze — disable protection for 15 min, 1 hour, or until tonight
- Costly override — turn off requires typing a reason + 24s wait

**Stats & Dashboard**
- Today dashboard with "Time reclaimed" hero stat
- Per-app open counts
- "You closed X of Y openings today" metric
- Empty state with encouraging message

**Onboarding**
- 2-step onboarding: intensity picker → permissions explainer
- Intensity presets: Gentle (5s), Balanced (8s), Focused (15s)
- Trust footer: "No internet · No account · Open source"

**Technical**
- Health check banner when protection stops unexpectedly
- OEM battery guide with manufacturer-specific instructions
- Dark mode support (follows system theme)
- Boot receiver to restart protection after reboot
- Room database with schema migrations
- 35 unit tests for the core rules engine

**Privacy**
- No INTERNET permission
- No analytics or telemetry
- All data stays on device

### Notes

This is the first public release. Future versions will add feed blocking (Reels/Shorts), daily limits, and schedules.

# Phase 0: Walking Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A sideloadable APK that detects when a hardcoded target app comes to the foreground and covers it with a crude overlay whose "Close" button works — proving detection latency, overlay-over-app, and service survival before any product code.

**Architecture:** Two Gradle modules: `app` (Android: FGS poller, overlay activity, permission screen) and `core` (pure Kotlin, JVM-tested). `MonitorService` polls `UsageStatsManager.queryEvents` ~900 ms while the screen is on, reduces events to the current foreground package via `core`, and launches `PauseActivity` over the target app.

**Tech Stack:** Kotlin 2.x, Jetpack Compose + Material 3, AGP 8.x, minSdk 26, compileSdk 35, JDK 17, JUnit for `core`, GitHub Actions CI.

**Spec:** `docs/superpowers/specs/2026-11-05-thumbstall-v0.1-design.md`

## Global Constraints

- `applicationId`: `app.thumbstall` (suffix `.debug` for debug builds)
- minSdk 26, no INTERNET permission in any manifest, no analytics/network deps
- Two flavors, dimension `distribution`: `foss` and `play` (identical in Phase 0)
- Permissions in Phase 0: `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`
- Hardcoded Phase 0 target app: `com.instagram.android` (constant, replaced by picker in Phase 1)
- Every failure fails open: any exception in the detection path is caught and logged, never blocks an app
- Monotonic time only (`SystemClock.elapsedRealtime()`) for intervals
- Commits: conventional style (`feat:`, `chore:`, `test:`, `ci:`, `docs:`)

## Review Focus

1. **Screen off** — polling must stop (battery drain is the silent uninstaller); resumes on screen on. Manual check in Task 7.
2. **Usage Access revoked mid-run** — `queryEvents` returns empty; service must not crash and must not block anything (fail open). Manual check in Task 7.
3. **Overlay launch loop** — repeated `ACTIVITY_RESUMED` events for the same package must not stack overlays: skip if already showing or same package within 1 s. Pinned by `core` test in Task 2.
4. **Close → immediate reopen** — after Close sends HOME, reopening the target must pause again (no stuck "already handled" state). Manual check in Task 7.
5. **Reboot** — service re-arms on `BOOT_COMPLETED`; without it, protection silently dies at first reboot. Manual check in Task 7.

---

### Task 1: Gradle scaffold (two modules, flavors, Compose)

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradlew` (wrapper)
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/app/thumbstall/MainActivity.kt`
- Create: `core/build.gradle.kts`
- Create: `.gitignore` (Android template)

**Interfaces:**
- Produces: `:app` (com.android.application, Compose enabled, flavors `foss`/`play`) depending on `:core` (org.jetbrains.kotlin.jvm, no Android deps)

- [ ] **Step 1: Generate the project**

Use current stable AGP/Kotlin via the version catalog. `:app` with Compose BOM + Material 3, flavor dimension `distribution` with `foss` and `play` (no config differences yet). `:core` as plain `kotlin("jvm")` with `testImplementation(kotlin("test"))`. `MainActivity` shows placeholder text "ThumbStall".

- [ ] **Step 2: Verify both flavors build and core tests run**

Run: `./gradlew assembleFossDebug assemblePlayDebug :core:test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "chore: scaffold app + core modules with foss/play flavors"
```

### Task 2: core — foreground event reduction + overlay de-dup rule

**Files:**
- Create: `core/src/main/kotlin/app/thumbstall/core/ForegroundEvent.kt`
- Create: `core/src/main/kotlin/app/thumbstall/core/OverlayGate.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/OverlayGateTest.kt`

**Interfaces:**
- Produces: `data class ForegroundEvent(val packageName: String, val elapsedMs: Long)`
- Produces: `class OverlayGate(private val cooldownMs: Long = 1000) { fun shouldLaunch(event: ForegroundEvent, targetPackages: Set<String>, overlayShowing: Boolean): Boolean }` — true only when the package is a target, no overlay is showing, and the same package hasn't fired within `cooldownMs` (keyed on `elapsedMs`, monotonic)

- [ ] **Step 1: Write failing tests**

```kotlin
class OverlayGateTest {
    @Test fun `launches for target package`() { /* gate.shouldLaunch(ev("com.instagram.android", 0), targets, false) == true */ }
    @Test fun `ignores non-target package`() { /* "com.android.chrome" -> false */ }
    @Test fun `skips when overlay already showing`() { /* overlayShowing = true -> false */ }
    @Test fun `skips same package within cooldown`() { /* fire at 0 -> true; fire at 500 -> false; fire at 1500 -> true */ }
    @Test fun `different target package not blocked by cooldown`() { /* A at 0 -> true; B at 500 -> true */ }
}
```

- [ ] **Step 2: Run tests, verify failure**

Run: `./gradlew :core:test`
Expected: FAIL (unresolved references)

- [ ] **Step 3: Implement `ForegroundEvent` and `OverlayGate`**

- [ ] **Step 4: Run tests, verify pass**

Run: `./gradlew :core:test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core && git commit -m "feat: overlay gate with target matching and launch de-dup"
```

### Task 3: Permission status screen

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/MainActivity.kt`
- Create: `app/src/main/kotlin/app/thumbstall/ui/PermissionScreen.kt`
- Create: `app/src/main/kotlin/app/thumbstall/Permissions.kt`

**Interfaces:**
- Produces: `object Permissions { fun hasUsageAccess(context: Context): Boolean; fun hasOverlay(context: Context): Boolean }` (AppOpsManager check / `Settings.canDrawOverlays`)
- Produces: `PermissionScreen` composable — one row per permission: label, granted ✓/✗, button firing `ACTION_USAGE_ACCESS_SETTINGS` / `ACTION_MANAGE_OVERLAY_PERMISSION`; a "Start protection" button enabled only when both are granted (wired to the service in Task 4)

- [ ] **Step 1: Implement `Permissions` and `PermissionScreen`, set as MainActivity content**

Re-check grant state in `onResume` (returning from Settings must refresh without restart).

- [ ] **Step 2: Build**

Run: `./gradlew assembleFossDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual device check**

Sideload, grant both permissions via the buttons, confirm both rows show granted after returning.

- [ ] **Step 4: Commit**

```bash
git add app && git commit -m "feat: permission status screen with settings deep links"
```

### Task 4: MonitorService — specialUse FGS with screen-gated polling

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`
- Create: `app/src/main/kotlin/app/thumbstall/usage/ForegroundPoller.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/kotlin/app/thumbstall/ui/PermissionScreen.kt` ("Start protection" starts the service)

**Interfaces:**
- Consumes: `ForegroundEvent` from Task 2
- Produces: `class ForegroundPoller(context) { fun poll(): ForegroundEvent? }` — wraps `UsageStatsManager.queryEvents`; keys on `ACTIVITY_RESUMED` (API 29+) / `MOVE_TO_FOREGROUND` (26–28); advances the query window only after a successful read; backdates the first window by 2 s; caches and suppresses repeats of the last package; converts wall timestamps to a `ForegroundEvent` stamped with `elapsedRealtime()`; any exception → log + return null (fail open)
- Produces: `MonitorService` — `specialUse` FGS (subtype property string: `"Monitors foreground app to show a user-configured pause screen"`), low-importance persistent notification "ThumbStall is protecting you", coroutine loop polling every 900 ms, registered `SCREEN_OFF`/`SCREEN_ON` receivers stopping/starting the loop, `startMonitor(context)`/`stopMonitor(context)` companions

- [ ] **Step 1: Implement `ForegroundPoller`**

- [ ] **Step 2: Implement `MonitorService`, declare in manifest with permissions from Global Constraints, wire "Start protection"**

For now the poll result is only logged: `Log.d("ThumbStall", "foreground: $pkg")`.

- [ ] **Step 3: Build**

Run: `./gradlew assembleFossDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Manual device check**

Start protection; `adb logcat -s ThumbStall` while switching between 3 apps shows each package within ~1.5 s; notification visible; screen off stops log lines, screen on resumes them.

- [ ] **Step 5: Commit**

```bash
git add app && git commit -m "feat: foreground monitor service with screen-gated UsageStats polling"
```

### Task 5: Crude pause overlay

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/overlay/PauseActivity.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `OverlayGate` from Task 2, poll loop from Task 4
- Produces: `PauseActivity` — translucent theme, `launchMode="singleInstance"`, `excludeFromRecents=true`, full-screen scrim with "ThumbStall" text and a filled **"Close Instagram"** button → `Intent(ACTION_MAIN).addCategory(CATEGORY_HOME)` + `finish()`; back press does nothing in Phase 0; static `var showing: Boolean` set in `onStart`/`onStop` feeding the gate
- Produces: `MonitorService` calls `OverlayGate.shouldLaunch(event, setOf("com.instagram.android"), PauseActivity.showing)` each poll; true → start `PauseActivity` (`FLAG_ACTIVITY_NEW_TASK`)

- [ ] **Step 1: Implement `PauseActivity` + manifest entry, wire the gate into the service loop**

- [ ] **Step 2: Build**

Run: `./gradlew assembleFossDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manual device check (THE Phase 0 exit criterion)**

Open Instagram → overlay covers it < 1.5 s → "Close Instagram" lands on home screen → reopen Instagram → overlay appears again. Chrome never triggers it.

- [ ] **Step 4: Commit**

```bash
git add app && git commit -m "feat: crude pause overlay over hardcoded target app"
```

### Task 6: Boot re-arm

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/service/BootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt` (persist "protection enabled" flag via SharedPreferences in `startMonitor`/`stopMonitor`)

**Interfaces:**
- Produces: `BootReceiver` — on `BOOT_COMPLETED`, if the enabled flag is set and both permissions still granted, `MonitorService.startMonitor(context)`; wrapped in try/catch (fail open)

- [ ] **Step 1: Implement receiver + flag, declare in manifest**

- [ ] **Step 2: Build, manual device check**

Run: `./gradlew assembleFossDebug`; reboot device with protection on → notification reappears and Instagram is paused without opening ThumbStall.

- [ ] **Step 3: Commit**

```bash
git add app && git commit -m "feat: re-arm monitor service after reboot"
```

### Task 7: CI, smoke checklist, signing key

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `TESTING.md`
- Create: `docs/signing.md`

**Interfaces:**
- Produces: CI running on push/PR: JDK 17 setup, `./gradlew assembleFossDebug assemblePlayDebug :core:test`

- [ ] **Step 1: Write `ci.yml`**

- [ ] **Step 2: Write `TESTING.md`** — the manual smoke checklist, verbatim from the Review Focus section plus the Task 4–6 device checks: detection < 1.5 s; Close works; immediate reopen re-pauses; screen off stops polling / on resumes; Usage Access revoked mid-run → no crash, apps open normally; reboot re-arms; notification present.

- [ ] **Step 3: Write `docs/signing.md`** — the `keytool -genkeypair` command for the release keystore, instruction to store it offline (never committed), and the `~/.gradle/gradle.properties` signing variable names for later release builds. Generating the key is a human step; CI signing lands in Phase 4.

- [ ] **Step 4: Push, verify CI green**

Run: `git add -A && git commit -m "ci: build flavors + core tests; add smoke checklist and signing doc" && git push`
Expected: green check on GitHub Actions.

- [ ] **Step 5: Run the full `TESTING.md` checklist on a real device — Phase 0 done**

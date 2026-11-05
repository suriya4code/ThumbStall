# Phase 1: The Pause, Done Properly — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded crude overlay with the real product pause: a TDD'd rules engine (escalation, essentials, monotonic time), the designed pause screen (`design_screens/04-pause-screen.png`), a user app picker, and events persisted to Room.

**Architecture:** `core` grows the pure-Kotlin rules (`PauseRules` + `OpenTracker`) deciding None vs Pause(seconds). `app` gains `data/` (Room events + DataStore settings), an app-picker screen, and a redesigned `PauseActivity`; `MonitorService` rewires from hardcoded targets to DataStore + rules + event logging.

**Tech Stack:** Existing stack + Room (KSP), DataStore Preferences. Versions must be compileSdk-36-compatible (see Global Constraints).

**Spec:** `docs/superpowers/specs/2026-11-05-thumbstall-v0.1-design.md` (esp. §2 features 1/3/4/5, §3, §7 design map)

## Global Constraints

- compileSdk 36 — any new androidx dep must not demand platform 37 (BOM 2026.06.01 / core-ktx 1.17.0 pins stay; verify new deps' AAR metadata at first build)
- Run Gradle with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (JDK 21, bytecode target 17)
- No INTERNET permission; no analytics; `core` stays free of Android deps
- Monotonic time (`elapsedRealtime`) for intervals; wall time only for day/week stats boundaries
- Fail open everywhere: rules/DB/DataStore errors → app opens normally
- Balanced preset values (spec §2): base pause 8 s (Gentle 5 / Focused 15 — enum exists now, picker arrives Phase 3; default Balanced); escalation +5 s per repeat open within rolling 60 min, capped at 30 s total
- Pinned copy (spec §7): "Still want to scroll?", "Close [app]", "Open for · available in Ns", "Nth open today", "You closed it instead N times this week"
- Palette: cream `0xFFF2F1E8` surfaces, olive `0xFF4A5D23` primary, pause screen background near-black olive `0xFF161A10`
- Commits: conventional style

## Review Focus

1. **Clock jump / timezone change** — escalation and attempt counts must not break when wall clock changes; intervals use elapsedMs only. Pinned by `OpenTracker` test (Task 1).
2. **DB write failure mid-pause** — pause must still show and Close must still work if Room insert throws. Pinned by try/catch around recording (Task 5) + manual check note.
3. **Picker selects an essential app** — essentials are filtered from the picker AND re-checked in rules, so a stale selection can never pause the dialer. Pinned by `PauseRules` essentials test (Task 1) + picker filter (Task 4).
4. **Zero selected apps** — service runs with empty targets without crashing or pausing anything. Pinned by `PauseRules` non-target test (Task 1); manual check (Task 7).
5. **Attempt counter at midnight** — "Nth open today" resets on the device's local day boundary. Pinned by `StatsWindows` test (Task 2).

---

### Task 1: core — PauseRules + OpenTracker (TDD)

**Files:**
- Create: `core/src/main/kotlin/app/thumbstall/core/PauseRules.kt` (also holds `PauseConfig`, `PauseDecision`)
- Create: `core/src/main/kotlin/app/thumbstall/core/OpenTracker.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/PauseRulesTest.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/OpenTrackerTest.kt`

**Interfaces:**
- Produces: `data class PauseConfig(val basePauseSeconds: Int = 8, val escalationStepSeconds: Int = 5, val escalationWindowMs: Long = 3_600_000L, val maxPauseSeconds: Int = 30)`
- Produces: `sealed interface PauseDecision { data object None : PauseDecision; data class Pause(val seconds: Int) : PauseDecision }`
- Produces: `class OpenTracker { fun recordOpen(packageName: String, elapsedMs: Long); fun opensWithin(packageName: String, windowMs: Long, nowElapsedMs: Long): Int }` — counts recorded opens with `nowElapsedMs - openElapsedMs < windowMs`; prunes stale entries
- Produces: `class PauseRules(private val config: PauseConfig = PauseConfig()) { fun decide(event: ForegroundEvent, targetPackages: Set<String>, essentialPackages: Set<String>, priorOpensInWindow: Int): PauseDecision }` — None for non-targets and for essentials (even when targeted); otherwise Pause(min(base + step × priorOpensInWindow, max))

- [ ] **Step 1: Write failing OpenTracker tests**

```kotlin
class OpenTrackerTest {
    @Test fun `no opens recorded means zero in window`() { /* opensWithin("a", 60_000, 0) == 0 */ }
    @Test fun `counts opens inside window`() { /* record a@0, a@10_000; opensWithin(a, 60_000, 20_000) == 2 */ }
    @Test fun `ignores opens outside window`() { /* record a@0; opensWithin(a, 60_000, 61_000) == 0 */ }
    @Test fun `windows are per package`() { /* record a@0; opensWithin(b, 60_000, 1_000) == 0 */ }
    @Test fun `monotonic timestamps immune to wall clock`() { /* only elapsedMs arithmetic — record a@5, query now=10 window 6 → 1 */ }
}
```

- [ ] **Step 2: Run, verify RED (unresolved references)** — `./gradlew :core:test`

- [ ] **Step 3: Implement `OpenTracker`; run, verify GREEN**

- [ ] **Step 4: Write failing PauseRules tests**

```kotlin
class PauseRulesTest {
    @Test fun `non-target gets no pause`() { /* decide(chrome, targets={ig}, ...) == None */ }
    @Test fun `target gets base pause`() { /* priorOpens=0 → Pause(8) */ }
    @Test fun `essential never paused even when targeted`() { /* pkg in both sets → None */ }
    @Test fun `each repeat open within window adds step`() { /* priorOpens=2 → Pause(18) */ }
    @Test fun `escalation caps at max`() { /* priorOpens=10 → Pause(30) */ }
    @Test fun `gentle and focused presets change base`() { /* PauseConfig(basePauseSeconds=5)→Pause(5); 15→Pause(15) */ }
}
```

- [ ] **Step 5: Run RED, implement `PauseRules`, run GREEN** — `./gradlew :core:test`, all core tests pass

- [ ] **Step 6: Commit**

```bash
git add core && git commit -m "feat: pause rules with escalation, essentials and monotonic open tracking"
```

### Task 2: core — stats window helpers (TDD)

**Files:**
- Create: `core/src/main/kotlin/app/thumbstall/core/StatsWindows.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/StatsWindowsTest.kt`

**Interfaces:**
- Produces: `object StatsWindows { fun todayStartMillis(nowWallMs: Long, zone: java.time.ZoneId): Long; fun weekStartMillis(nowWallMs: Long, zone: java.time.ZoneId): Long }` — local midnight today, and local midnight 6 days before today (rolling 7-day window). java.time is JDK, allowed in `core`.

- [ ] **Step 1: Write failing tests** — fixed zone (e.g., `ZoneId.of("Asia/Kolkata")`): now at 2026-11-05T13:00 local → todayStart = 2026-11-05T00:00 local millis; 00:05 after midnight rolls over; weekStart = 2026-10-30T00:00 local.

- [ ] **Step 2: RED → implement → GREEN** — `./gradlew :core:test`

- [ ] **Step 3: Commit**

```bash
git add core && git commit -m "feat: local-day and rolling-week stat window helpers"
```

### Task 3: data — Room events + DataStore settings

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/data/PauseEvent.kt`
- Create: `app/src/main/kotlin/app/thumbstall/data/EventDao.kt`
- Create: `app/src/main/kotlin/app/thumbstall/data/AppDatabase.kt`
- Create: `app/src/main/kotlin/app/thumbstall/data/SettingsStore.kt`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts` (Room + KSP + DataStore; `room { schemaDirectory("$projectDir/schemas") }`, commit schema JSON)

**Interfaces:**
- Produces: `@Entity data class PauseEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val packageName: String, val type: String, val wallTimeMs: Long)` with type constants `TYPE_PAUSED / TYPE_DISMISSED / TYPE_CONTINUED` (companion)
- Produces: `@Dao interface EventDao { @Insert suspend fun insert(event: PauseEvent); @Query(...) suspend fun countSince(packageName: String, type: String, sinceWallMs: Long): Int }`
- Produces: `AppDatabase.get(context): AppDatabase` singleton, db name `thumbstall.db`, version 1, `exportSchema = true`
- Produces: `class SettingsStore(context)` — DataStore Preferences: `suspend fun targetPackages(): Set<String>`, `suspend fun setTargetPackages(pkgs: Set<String>)`, `suspend fun intensity(): String` (default `"balanced"`)

- [ ] **Step 1: Add deps (room-runtime, room-ktx, room-compiler via KSP, datastore-preferences), sync versions against compileSdk 36**

Run: `./gradlew assembleFossDebug` — if AAR metadata demands platform 37, pick the newest Room/DataStore versions that don't.
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Implement entity/DAO/db/settings store; build; commit schema JSON**

Run: `./gradlew assembleFossDebug`; verify `app/schemas/app.thumbstall.data.AppDatabase/1.json` exists.
Expected: BUILD SUCCESSFUL, schema file committed

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: room event store and datastore settings"
```

### Task 4: App picker

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/AppPickerScreen.kt`
- Create: `app/src/main/kotlin/app/thumbstall/EssentialApps.kt`
- Modify: `app/src/main/AndroidManifest.xml` (`<queries>` LAUNCHER intent)
- Modify: `app/src/main/kotlin/app/thumbstall/MainActivity.kt` (simple two-destination navigation: permissions ↔ picker; a "Choose apps" button)

**Interfaces:**
- Produces: `object EssentialApps { fun resolve(context: Context): Set<String> }` — default dialer (`TelecomManager.defaultDialerPackage`), default SMS (`Telephony.Sms.getDefaultSmsPackage`), own package, plus static set: `com.google.android.dialer`, `com.android.phone`, `com.google.android.apps.maps`, `com.google.android.GoogleCamera`, `com.google.android.apps.authenticator2`, `com.android.settings`
- Produces: `AppPickerScreen(apps, selected, onToggle, onDone)` — launchable apps via `packageManager.queryIntentActivities(MAIN/LAUNCHER)`, essentials and own package filtered out, sorted by label, checkbox rows (icon, label), selection persisted through `SettingsStore.setTargetPackages`

- [ ] **Step 1: Add `<queries>` and implement `EssentialApps` + picker + navigation wiring**

- [ ] **Step 2: Build + manual device check**

Run: `./gradlew assembleFossDebug`, install on emulator.
Expected: picker lists launcher apps with labels/icons (package visibility works), Chrome selectable, Settings/dialer absent, selection survives app restart.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: app picker with essentials filtered and datastore persistence"
```

### Task 5: Service rewiring — rules + persistence

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`
- Delete: hardcoded `TARGET_PACKAGES` constant (and its BuildConfig debug branch)
- Modify: `app/src/main/kotlin/app/thumbstall/overlay/PauseActivity.kt` (new extras only — full redesign is Task 6)

**Interfaces:**
- Consumes: `PauseRules`, `OpenTracker`, `PauseConfig` (Task 1); `AppDatabase`/`EventDao`/`PauseEvent` (Task 3); `SettingsStore.targetPackages()` (Task 3); `EssentialApps.resolve` (Task 4)
- Produces: service flow per poll event: targets ← SettingsStore (cached, refreshed each service start and every 30 polls); `priorOpens = openTracker.opensWithin(pkg, config.escalationWindowMs, event.elapsedMs)`; `rules.decide(...)`; on `Pause(seconds)` → `openTracker.recordOpen`, insert `TYPE_PAUSED` event (try/catch, fail open), launch `PauseActivity` with extras `EXTRA_TARGET_PACKAGE`, `EXTRA_PAUSE_SECONDS`
- Produces: `OverlayGate` stays as the launch de-dup in front of the rules

- [ ] **Step 1: Rewire the poll loop; config from intensity (`gentle`→5, `balanced`→8, `focused`→15 base seconds)**

- [ ] **Step 2: Build + manual device check**

Run: `./gradlew assembleFossDebug`, install; select Chrome in picker, start protection.
Expected: Chrome pauses; second open within the hour gets a visibly longer countdown (extras carry seconds); Settings never pauses; with zero apps selected nothing pauses and nothing crashes.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: monitor service drives pause from rules, settings and event log"
```

### Task 6: Pause screen per design 04

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/theme/Theme.kt` (cream/olive `MaterialTheme` color scheme + pause-screen dark palette)
- Rewrite: `app/src/main/kotlin/app/thumbstall/overlay/PauseActivity.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/MainActivity.kt`, `app/src/main/kotlin/app/thumbstall/ui/PermissionScreen.kt` (apply theme)

**Interfaces:**
- Consumes: `EXTRA_PAUSE_SECONDS`, `EXTRA_TARGET_PACKAGE` (Task 5); `EventDao.countSince` + `StatsWindows` (Tasks 2–3)
- Produces: pause screen on `0xFF161A10`: header = app label + "Nth open today" (`countSince(pkg, TYPE_PAUSED, todayStart)`); center breathing circle animating scale 1f↔1.15f with live countdown number and "breathe in"/"breathe out" alternating每 4 s; "Still want to scroll?" headline + supporting line; footer: during countdown "Open for · available in Ns" with disabled 5/10/15 min chips; at 0 chips enable — **any chip just finishes the activity in Phase 1** (budget enforcement is Phase 2) and logs `TYPE_CONTINUED`; filled cream "Close [app]" button always enabled → logs `TYPE_DISMISSED`, HOME intent, finish; bottom caption "You closed it instead N times this week" (`countSince(pkg, TYPE_DISMISSED, weekStart)`)
- Produces: back press still does nothing; `showing` flag unchanged

- [ ] **Step 1: Implement theme + screen; countdown driven by a `LaunchedEffect` ticking on elapsed time (not wall clock)**

- [ ] **Step 2: Build + visual check against `design_screens/04-pause-screen.png` on emulator; screenshot both countdown and post-countdown states**

Run: `./gradlew assembleFossDebug`
Expected: layout/copy match the design; chips disabled until countdown ends; counters show real numbers after a few opens/closes.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: designed pause screen with breathing countdown and attempt counters"
```

### Task 7: Phase exit — full suite, device pass, checklist update

**Files:**
- Modify: `TESTING.md` (picker rows: selection persists, essentials absent; escalation row: repeat open → longer pause; counters row: Nth open / closed-this-week increment)

- [ ] **Step 1: Run full verification**

Run: `./gradlew :core:test assembleFossDebug assemblePlayDebug`
Expected: all core tests pass (old 5 + new ~16), both flavors build.

- [ ] **Step 2: Manual device pass of the updated TESTING.md core-loop sections on emulator** — detection, pause with countdown, Close→home, reopen escalates, counters increment, zero-selection safe.

- [ ] **Step 3: Commit + push branch, verify CI green**

```bash
git add -A && git commit -m "test: extend smoke checklist for picker, escalation and counters" && git push -u origin phase-1
```

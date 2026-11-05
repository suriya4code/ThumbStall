# Phase 2: Snooze & Session Budget — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add snooze (global pause of protection with auto-resume) and session budget (per-app "Open for N minutes" that expires). Both use monotonic elapsed time for correctness across clock changes.

**Design refs:** `design_screens/06-snooze-sheet.png` (snooze UI), `design_screens/04-pause-screen.png` (5/10/15 chips grant budget)

**Spec:** `docs/superpowers/specs/2026-11-05-thumbstall-v0.1-design.md` §2 features 6 (snooze) and 4 (costly override / session budget)

## Architecture

```text
core/
  BudgetTracker       # TDD: per-pkg budget expiry using elapsedMs
  SnoozeState         # TDD: snooze expiry using elapsedMs

app/
  data/
    SettingsStore     # add snooze fields (elapsedMs expiry, wall time for display)
  ui/
    SnoozeSheet       # bottom sheet: 15m/1h/tonight options, turn-off with reason
  overlay/
    PauseActivity     # chips call BudgetTracker.grant(); show remaining budget
  service/
    MonitorService    # skip pause if snooze active OR budget active for pkg
    SnoozeScheduler   # AlarmManager exact alarm to resume after snooze
```

## Global Constraints

- Monotonic time (`SystemClock.elapsedRealtime()`) for all interval math
- Wall time only for display ("Back on at 2:40 PM") and "until tonight" calculation
- Snooze survives reboot via persisted wall-time target + recalc on boot
- Budget is in-memory only (lost on process death — acceptable for short durations)
- Fail open: expired/missing state → protection on, no budget

## Review Focus

1. **Clock jump** — snooze and budget must not break when wall clock changes; pinned by TDD tests using only elapsedMs.
2. **Reboot during snooze** — snooze must resume correctly; pinned by SnoozeScheduler re-arm in BootReceiver + wall-time persistence.
3. **Budget overflow** — granting 15 min while 5 min remains should extend, not stack; pinned by BudgetTracker test.
4. **"Until tonight"** — must use device timezone for 9 PM; pinned by SnoozeState test with fixed zone.

---

### Task 1: core — BudgetTracker (TDD)

**Files:**
- Create: `core/src/main/kotlin/app/thumbstall/core/BudgetTracker.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/BudgetTrackerTest.kt`

**Interfaces:**
- Produces: `class BudgetTracker { fun grant(packageName: String, durationMs: Long, nowElapsedMs: Long); fun hasActiveBudget(packageName: String, nowElapsedMs: Long): Boolean; fun remainingMs(packageName: String, nowElapsedMs: Long): Long }`
- `grant` sets expiry = nowElapsedMs + durationMs (overwrites any existing budget for that pkg)
- `hasActiveBudget` returns true if expiry > nowElapsedMs
- `remainingMs` returns max(0, expiry - nowElapsedMs)

- [ ] **Step 1: Write failing tests**

```kotlin
class BudgetTrackerTest {
    @Test fun `no budget means not active`()
    @Test fun `granted budget is active within duration`()
    @Test fun `budget expires after duration`()
    @Test fun `budgets are per package`()
    @Test fun `granting new budget overwrites previous`()
    @Test fun `remainingMs returns correct value`()
}
```

- [ ] **Step 2: RED → implement → GREEN** — `./gradlew :core:test`

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: budget tracker with per-package expiry"
```

### Task 2: core — SnoozeState (TDD)

**Files:**
- Create: `core/src/main/kotlin/app/thumbstall/core/SnoozeState.kt`
- Test: `core/src/test/kotlin/app/thumbstall/core/SnoozeStateTest.kt`

**Interfaces:**
- Produces: `data class SnoozeState(val expiryElapsedMs: Long, val expiryWallMs: Long)`
- Produces: `object SnoozeCalc { fun forDuration(durationMs: Long, nowElapsedMs: Long, nowWallMs: Long): SnoozeState; fun untilTonight(nowElapsedMs: Long, nowWallMs: Long, zone: ZoneId): SnoozeState; fun isActive(state: SnoozeState?, nowElapsedMs: Long): Boolean }`
- `untilTonight` computes 9 PM local today (or now+1min if past 9 PM)

- [ ] **Step 1: Write failing tests**

```kotlin
class SnoozeStateTest {
    @Test fun `forDuration computes correct expiry`()
    @Test fun `untilTonight returns 9 PM local`()
    @Test fun `untilTonight after 9 PM returns now plus 1 minute`()
    @Test fun `isActive true before expiry`()
    @Test fun `isActive false after expiry`()
    @Test fun `isActive false when null`()
}
```

- [ ] **Step 2: RED → implement → GREEN**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: snooze state with until-tonight calculation"
```

### Task 3: data — SettingsStore snooze fields

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/data/SettingsStore.kt`

**Interfaces:**
- Adds: `suspend fun snoozeExpiryWallMs(): Long?`, `suspend fun setSnooze(expiryWallMs: Long)`, `suspend fun clearSnooze()`
- Wall time persisted so snooze survives reboot; elapsed-time expiry recalculated on read

- [ ] **Step 1: Add snooze fields to DataStore**

- [ ] **Step 2: Build** — `./gradlew assembleFossDebug`

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: persist snooze expiry in datastore"
```

### Task 4: SnoozeSheet UI

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/SnoozeSheet.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/MainActivity.kt` (add snooze entry point in PermissionScreen or a Settings tab)

**Interfaces:**
- Produces: `@Composable fun SnoozeSheet(onSnooze: (durationMs: Long) -> Unit, onUntilTonight: () -> Unit, onTurnOff: (reason: String) -> Unit, onDismiss: () -> Unit)`
- Radio-style selection: 15 min / 1 hour / Until tonight, each showing "Back on at <time>"
- "Snooze for X" primary button
- "Turn protection off" section: text field for reason, "Turn off in 0:24" button (24s countdown, disabled until reason entered)

- [ ] **Step 1: Implement SnoozeSheet composable**

- [ ] **Step 2: Wire into MainActivity** — add a "Snooze" button when protection is running; show sheet as modal bottom sheet

- [ ] **Step 3: Build + visual check against design**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: snooze sheet with duration options and turn-off"
```

### Task 5: SnoozeScheduler + service integration

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/service/SnoozeScheduler.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/service/BootReceiver.kt`

**Interfaces:**
- Produces: `object SnoozeScheduler { fun schedule(context: Context, expiryWallMs: Long); fun cancel(context: Context) }` — uses `AlarmManager.setExactAndAllowWhileIdle` to fire a broadcast that calls `MonitorService.startMonitor`
- MonitorService: on poll, if `SnoozeCalc.isActive(snoozeState, nowElapsed)` → skip all pauses
- BootReceiver: if snooze persisted and not expired → recalc elapsed expiry, re-schedule alarm

- [ ] **Step 1: Implement SnoozeScheduler with BroadcastReceiver**

- [ ] **Step 2: Wire MonitorService to skip pauses during snooze**

- [ ] **Step 3: Wire BootReceiver to re-arm snooze alarm**

- [ ] **Step 4: Build + manual test** — snooze 15 min, verify no pauses, verify protection resumes

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: snooze scheduler with alarm and boot re-arm"
```

### Task 6: Budget integration — pause screen + service

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/overlay/PauseActivity.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`

**Interfaces:**
- PauseActivity: chips call `BudgetTracker.grant(pkg, minutes * 60_000L, elapsedNow)`; tracker instance held in MonitorService (singleton access or passed via intent is tricky — use Application-scoped singleton)
- Create: `app/src/main/kotlin/app/thumbstall/ThumbStallApp.kt` — Application subclass holding `BudgetTracker` instance
- MonitorService: on poll, if `budgetTracker.hasActiveBudget(pkg, nowElapsed)` → skip pause for that pkg
- PauseActivity: if budget active on entry (edge case: overlay still showing when budget granted elsewhere), show remaining time and auto-dismiss

- [ ] **Step 1: Create ThumbStallApp with BudgetTracker singleton**

- [ ] **Step 2: Wire PauseActivity chips to grant budget**

- [ ] **Step 3: Wire MonitorService to skip pause when budget active**

- [ ] **Step 4: Build + manual test** — tap 5 min chip, verify app opens, verify pause returns after 5 min

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: session budget grants via pause screen chips"
```

### Task 7: Phase exit — full suite, checklist update, PR

**Files:**
- Modify: `TESTING.md`

- [ ] **Step 1: Run full verification**

```bash
./gradlew :core:test assembleFossDebug assemblePlayDebug
```

Expected: all core tests pass (21 existing + ~12 new = ~33), both flavors build.

- [ ] **Step 2: Update TESTING.md** — add snooze rows (snooze 15 min → no pauses → resumes), budget rows (tap chip → app opens → pause returns after expiry)

- [ ] **Step 3: Manual device pass on emulator**

- [ ] **Step 4: Commit + push + PR**

```bash
git commit -m "test: extend checklist for snooze and budget"
git push -u origin phase-2
```

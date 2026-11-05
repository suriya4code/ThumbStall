# Phase 3: Onboarding & Today Dashboard — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the bare setup screen with proper 2-step onboarding (intensity → permissions) and a Today dashboard showing stats. This is the last v0.1 UI phase — after this the app is feature-complete for first release.

**Design refs:**
- `design_screens/01-onboarding-intensity.png` — intensity picker
- `design_screens/02-onboarding-permissions.png` — permissions explainer
- `design_screens/03-today-dashboard.png` — Today tab

**Spec:** `docs/superpowers/specs/2026-11-05-thumbstall-v0.1-design.md` §2 features 2 (2-min onboarding), 8 (positive stats), 10 (health check/OEM guide)

## Architecture

```text
app/
  data/
    SettingsStore       # +onboardingComplete flag
  ui/
    OnboardingIntensity # Step 1: Gentle/Balanced/Focused cards
    OnboardingPerms     # Step 2: permissions with chips + trust footer
    TodayScreen         # dashboard: status pill, time reclaimed, per-app list
    MainNavigation      # Today + Settings tabs (Rules/Review deferred)
  MainActivity          # routes: onboarding flow OR main tabs
```

## Global Constraints

- First launch → onboarding flow; subsequent → Today tab
- Intensity stored in DataStore (already exists), onboardingComplete flag added
- Time reclaimed = sum of pause durations where user chose Close (TYPE_DISMISSED events × pauseSeconds at time of event — requires schema change to store pause duration)
- Per-app list shows apps with events today, sorted by open count desc
- OEM keep-alive guide is a simple expandable card with manufacturer-specific instructions (static text, no deep links yet)
- Trust footer: "No internet access · No account · Open source"

## Review Focus

1. **Onboarding re-entry** — if user kills app mid-onboarding, they should resume where they left off (or restart cleanly).
2. **Time reclaimed accuracy** — must use actual pause durations, not estimates; requires storing pauseSeconds with each event.
3. **Empty state** — Today screen before any pauses should show encouraging copy, not zeros.

---

### Task 1: Schema v2 — add pauseSeconds to PauseEvent

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/data/PauseEvent.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/data/AppDatabase.kt` (version 2, migration)
- Modify: `app/src/main/kotlin/app/thumbstall/data/EventDao.kt` (query for time reclaimed)
- Update: `app/schemas/` (new schema JSON)

**Interfaces:**
- Adds: `pauseSeconds: Int` column to PauseEvent (default 0 for migration)
- Adds: `@Query("SELECT SUM(pauseSeconds) FROM pause_events WHERE type = 'dismissed' AND wallTimeMs >= :sinceWallMs") suspend fun totalDismissedSeconds(sinceWallMs: Long): Int?`
- Adds: `@Query("SELECT packageName, COUNT(*) as count FROM pause_events WHERE type = 'paused' AND wallTimeMs >= :sinceWallMs GROUP BY packageName ORDER BY count DESC") suspend fun appOpenCounts(sinceWallMs: Long): List<AppOpenCount>`
- Adds: `data class AppOpenCount(val packageName: String, val count: Int)`

- [ ] **Step 1: Add column + migration + queries**

- [ ] **Step 2: Update PauseActivity and MonitorService to pass pauseSeconds when logging**

- [ ] **Step 3: Build, verify migration runs** — `./gradlew assembleFossDebug`

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: schema v2 adds pauseSeconds for time-reclaimed stats"
```

### Task 2: SettingsStore — onboardingComplete flag

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/data/SettingsStore.kt`

**Interfaces:**
- Adds: `suspend fun isOnboardingComplete(): Boolean`, `suspend fun setOnboardingComplete()`

- [ ] **Step 1: Add flag**

- [ ] **Step 2: Build**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: onboarding complete flag in datastore"
```

### Task 3: OnboardingIntensity screen

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/OnboardingIntensity.kt`

**Interfaces:**
- Produces: `@Composable fun OnboardingIntensity(onSelect: (intensity: String) -> Unit)`
- Three cards: Gentle (5s), Balanced (8s, recommended chip), Focused (15s)
- Each card shows: name, description, "N seconds before you can continue"
- Bottom note: "Why a few seconds? Research shows..." (brief copy from design)
- Tapping a card calls onSelect with "gentle"/"balanced"/"focused"

- [ ] **Step 1: Implement screen per design 01**

- [ ] **Step 2: Build + visual check**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: onboarding intensity picker screen"
```

### Task 4: OnboardingPerms screen

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/OnboardingPerms.kt`

**Interfaces:**
- Produces: `@Composable fun OnboardingPerms(state: PermissionState, onRequestUsage: () -> Unit, onRequestOverlay: () -> Unit, onContinue: () -> Unit)`
- Header: "What ThumbStall needs, and why"
- Permission cards with chips: Usage Access (Needed), Display over apps (Needed), Keep running (Recommended — links to OEM guide, not a system permission)
- Each card: icon, title, description, chip, Grant/Granted button
- Trust footer: "No internet access · No account · Open source"
- Continue button enabled when both Needed permissions granted

- [ ] **Step 1: Implement screen per design 02**

- [ ] **Step 2: Build + visual check**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: onboarding permissions explainer screen"
```

### Task 5: TodayScreen — dashboard

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/TodayScreen.kt`

**Interfaces:**
- Produces: `@Composable fun TodayScreen(onChooseApps: () -> Unit, onSnooze: () -> Unit, onStop: () -> Unit)`
- Status pill: "Protection is on · N apps guarded" or "Protection is off"
- Hero stat card: "Time reclaimed" with large formatted duration (e.g., "2 h 15 min")
- Secondary stat: "You closed X of Y openings today"
- Per-app list: app icon, label, "N opens today", sorted by count desc
- Empty state: "Start using ThumbStall to see your stats here"
- Bottom: Snooze/Stop buttons when protection running, Start button when off

- [ ] **Step 1: Implement screen per design 03** (skip short-video allowance bar — that's deferred)

- [ ] **Step 2: Build + visual check**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: today dashboard with time reclaimed and per-app stats"
```

### Task 6: MainNavigation — tab scaffold

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/MainNavigation.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/MainActivity.kt`

**Interfaces:**
- Produces: `@Composable fun MainNavigation(...)` — bottom nav with Today + Settings tabs (Rules/Review tabs shown in design are deferred)
- Today tab → TodayScreen
- Settings tab → current PermissionScreen refactored as SettingsScreen (app picker, permissions status, intensity change, about)
- MainActivity: if onboardingComplete → MainNavigation; else → onboarding flow (intensity → perms → set complete → main)

- [ ] **Step 1: Implement tab scaffold**

- [ ] **Step 2: Wire MainActivity routing**

- [ ] **Step 3: Build + flow check** — fresh install goes through onboarding, subsequent launches go to Today

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: main navigation with Today and Settings tabs"
```

### Task 7: Phase exit — full suite, checklist, PR

**Files:**
- Modify: `TESTING.md`

- [ ] **Step 1: Run full verification**

```bash
./gradlew :core:test assembleFossDebug assemblePlayDebug
```

- [ ] **Step 2: Update TESTING.md** — onboarding rows (intensity → perms → Today), dashboard rows (time reclaimed updates, per-app list)

- [ ] **Step 3: Commit + push + PR**

```bash
git commit -m "test: extend checklist for onboarding and dashboard"
git push -u origin phase-3
```

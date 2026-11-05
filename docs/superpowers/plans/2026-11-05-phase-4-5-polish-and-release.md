# Phase 4+5: Polish & First Release — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Final polish (dark mode, accessibility, health check) and release prep (README, LICENSE, signed APK, GitHub Release). After this, v0.1.0 is public.

**Spec:** `docs/superpowers/specs/2026-11-05-thumbstall-v0.1-design.md` §4 Phase 4, §6 Release Discipline

## Scope

### Phase 4 — Polish
1. Dark mode support (Material 3 dynamic color + dark theme)
2. Health check banner (detect if service stopped unexpectedly)
3. OEM battery guide (expandable card with manufacturer-specific instructions)
4. TalkBack labels (contentDescription on interactive elements)
5. Minor UI polish

### Phase 5 — Release
1. LICENSE file (GPL-3.0)
2. README.md with features, screenshots placeholder, install instructions
3. CONTRIBUTING.md
4. CHANGELOG.md
5. Release signing (keystore already exists per docs/signing.md)
6. Tag v0.1.0 → GitHub Release

---

### Task 1: Dark mode support

**Files:**
- Modify: `app/src/main/kotlin/app/thumbstall/ui/theme/Theme.kt`
- Modify: `app/src/main/AndroidManifest.xml` (remove hardcoded light theme)

**Interfaces:**
- `ThumbStallTheme` detects system dark mode via `isSystemInDarkTheme()`
- Dark color scheme uses darker cream tones + same olive primary
- Pause screen already has its own dark theme (`PauseTheme`)

- [ ] **Step 1: Add dark color scheme + auto-detect**

- [ ] **Step 2: Update manifest to use Material3 theme**

- [ ] **Step 3: Build + visual check in dark mode**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: dark mode support with system detection"
```

### Task 2: Health check banner

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/HealthBanner.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/ui/TodayScreen.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/service/MonitorService.kt`

**Interfaces:**
- `MonitorService` writes last-alive timestamp to SharedPreferences every 30 polls (~27s)
- `HealthBanner` composable shows warning if: protection enabled but last-alive > 2 minutes ago
- Banner: "Protection may have stopped" with "Restart" button that calls `MonitorService.startMonitor`

- [ ] **Step 1: Add heartbeat to MonitorService**

- [ ] **Step 2: Create HealthBanner composable**

- [ ] **Step 3: Show banner in TodayScreen when stale**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: health check banner detects stopped protection"
```

### Task 3: OEM battery guide

**Files:**
- Create: `app/src/main/kotlin/app/thumbstall/ui/OemGuide.kt`
- Modify: `app/src/main/kotlin/app/thumbstall/ui/SettingsScreen.kt`

**Interfaces:**
- `OemGuide` composable: expandable card with manufacturer-specific instructions
- Detect manufacturer via `Build.MANUFACTURER`
- Instructions for: Samsung (battery optimization), Xiaomi (autostart + battery saver), OnePlus (battery optimization), Huawei (app launch), generic fallback
- Link to dontkillmyapp.com as fallback

- [ ] **Step 1: Create OemGuide with manufacturer detection**

- [ ] **Step 2: Add to SettingsScreen**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: OEM battery guide with manufacturer-specific instructions"
```

### Task 4: Accessibility labels

**Files:**
- Audit all UI files for missing `contentDescription`
- Add semantic descriptions to icons, buttons, status indicators

- [ ] **Step 1: Add contentDescription to icons and interactive elements**

- [ ] **Step 2: Test with TalkBack (manual)**

- [ ] **Step 3: Commit**

```bash
git commit -m "a11y: add TalkBack labels to interactive elements"
```

### Task 5: LICENSE + README + CONTRIBUTING

**Files:**
- Create: `LICENSE` (GPL-3.0 full text)
- Rewrite: `README.md` (features, install, build, screenshots placeholder)
- Create: `CONTRIBUTING.md`
- Create: `CHANGELOG.md`

- [ ] **Step 1: Add LICENSE (GPL-3.0)**

- [ ] **Step 2: Write README.md**

- [ ] **Step 3: Write CONTRIBUTING.md**

- [ ] **Step 4: Write CHANGELOG.md for v0.1.0**

- [ ] **Step 5: Commit**

```bash
git commit -m "docs: add LICENSE, README, CONTRIBUTING, CHANGELOG for v0.1.0"
```

### Task 6: Release build + tag

**Files:**
- Verify: `docs/signing.md` keystore exists
- Modify: `app/build.gradle.kts` (versionName "0.1.0")

- [ ] **Step 1: Update versionName to "0.1.0"**

- [ ] **Step 2: Build release APK**

```bash
./gradlew assembleFossRelease
```

- [ ] **Step 3: Verify APK is signed**

- [ ] **Step 4: Commit version bump**

```bash
git commit -m "chore: bump version to 0.1.0"
```

- [ ] **Step 5: Push, create PR, merge**

- [ ] **Step 6: Tag and create GitHub Release**

```bash
git tag v0.1.0
git push origin v0.1.0
gh release create v0.1.0 --title "v0.1.0 — First Release" --notes-file CHANGELOG.md app/build/outputs/apk/foss/release/app-foss-release.apk
```

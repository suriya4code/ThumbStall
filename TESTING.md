# Manual smoke checklist

Run on a real device before each tagged release. Every box must pass.
(Target app = any app in the configured target set; debug builds also
target Chrome so the emulator can run this list.)

## Onboarding (first launch)

- [ ] Fresh install shows intensity picker (Gentle / Balanced / Focused)
- [ ] Tapping a card proceeds to permissions screen
- [ ] Permissions screen shows Usage Access and Overlay as "Needed"
- [ ] Continue button enables only when both permissions granted
- [ ] After Continue, app shows Today tab (onboarding complete)
- [ ] Subsequent launches skip onboarding and show Today tab

## Setup (post-onboarding)

- [ ] Settings tab shows permission status with ✓/✗
- [ ] "Change" on apps row opens the picker; selected count displays
- [ ] "Start protection" enables only when permissions granted AND at least 1 app selected
- [ ] Persistent notification "ThumbStall is protecting you" appears
- [ ] OEM battery guide shows expandable card with manufacturer-specific instructions

## Dark mode

- [ ] Switch system to dark mode → app theme changes
- [ ] Switch system to light mode → app theme changes back
- [ ] Pause screen uses dark theme in both modes

## App picker

- [ ] Picker lists launchable apps with icons and labels
- [ ] Essential apps (dialer, SMS, Settings, own app) are absent from the list
- [ ] Selection persists after app restart
- [ ] Zero apps selected → Start protection stays disabled; service does not crash

## Core loop

- [ ] Open a selected app → pause overlay covers it in < 1.5 s
- [ ] Pause screen shows breathing countdown circle, "Still want to scroll?"
- [ ] 5/10/15 min chips are disabled during countdown, enabled after
- [ ] "Close [app]" button (always enabled) lands on the home screen
- [ ] Tapping any chip after countdown ends finishes the pause activity
- [ ] Immediately reopening the target app shows the overlay again
      (no stuck "already handled" state)
- [ ] A non-selected app (e.g., Settings) never triggers the overlay

## Escalation

- [ ] Second open within the hour shows a longer countdown than the first
- [ ] "Nth open today" counter increments across pauses
- [ ] "You closed it instead N times this week" appears after using Close button

## Session budget

- [ ] Tap a chip (5/10/15 min) after countdown → pause activity closes, app opens
- [ ] While budget active, reopening the same app does NOT show pause
- [ ] After budget expires, reopening the app shows pause again
- [ ] Budget is per-app: budget on Chrome doesn't affect Instagram

## Snooze

- [ ] When protection is running, "Snooze" button appears on main screen
- [ ] Snooze sheet shows 15 min / 1 hour / Until tonight with "Back on at" times
- [ ] Selecting an option and tapping Snooze closes the sheet
- [ ] During snooze, no apps are paused (all selected apps open normally)
- [ ] After snooze expires, protection resumes and apps are paused again
- [ ] "Turn off" requires typing a reason and waiting 24 seconds
- [ ] Reboot during snooze → snooze continues, protection resumes after expiry

## Today dashboard

- [ ] Status pill shows "Protection is on · N apps guarded" when running
- [ ] Status pill shows "Protection is off" when stopped
- [ ] "Time reclaimed" card shows cumulative pause seconds for closes today
- [ ] "You closed X of Y openings today" updates after using Close button
- [ ] Per-app list shows apps sorted by open count descending
- [ ] Empty state shows encouraging message before any pauses

## Failure modes (fail open)

- [ ] Screen off → polling stops (no `ThumbStall` logcat lines);
      screen on → resumes
- [ ] Revoke Usage Access while protection is running → no crash,
      all apps open normally (protection silently off)
- [ ] Phone call while the overlay is showing → overlay does not block the
      call UI

## Reboot

- [ ] Reboot with protection on → notification reappears and the target app
      is paused without opening ThumbStall (BOOT_COMPLETED delivery is
      deferred — wait up to a minute after boot)

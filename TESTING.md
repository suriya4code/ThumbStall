# Manual smoke checklist

Run on a real device before each tagged release. Every box must pass.
(Target app = any app in the configured target set; debug builds also
target Chrome so the emulator can run this list.)

## Setup

- [ ] Fresh install (or upgrade install) succeeds
- [ ] Both permission rows show ✗ before granting, ✓ after, state refreshes
      on returning from Settings without an app restart
- [ ] "Choose apps to pause" opens the picker; selected count displays
- [ ] "Start protection" enables only when permissions granted AND at least 1 app selected
- [ ] Persistent notification "ThumbStall is protecting you" appears

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

## Failure modes (fail open)

- [ ] Screen off → polling stops (no `ThumbStall` logcat lines);
      screen on → resumes
- [ ] Revoke Usage Access while protection is running → no crash,
      all apps open normally (protection silently off; health banner is Phase 3)
- [ ] Phone call while the overlay is showing → overlay does not block the
      call UI (full call handling is Phase 2)

## Reboot

- [ ] Reboot with protection on → notification reappears and the target app
      is paused without opening ThumbStall (BOOT_COMPLETED delivery is
      deferred — wait up to a minute after boot)

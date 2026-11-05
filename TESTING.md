# Manual smoke checklist

Run on a real device before each tagged release. Every box must pass.
(Target app = any app in the configured target set; debug builds also
target Chrome so the emulator can run this list.)

## Setup

- [ ] Fresh install (or upgrade install) succeeds
- [ ] Both permission rows show ✗ before granting, ✓ after, state refreshes
      on returning from Settings without an app restart
- [ ] "Start protection" enables only when both permissions are granted
- [ ] Persistent notification "ThumbStall is protecting you" appears

## Core loop

- [ ] Open the target app → pause overlay covers it in < 1.5 s
- [ ] "Close app" lands on the home screen
- [ ] Immediately reopening the target app shows the overlay again
      (no stuck "already handled" state)
- [ ] A non-target app (e.g., Settings) never triggers the overlay

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

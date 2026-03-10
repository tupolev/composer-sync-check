# Configuration

Composer Sync Check settings are available at:

`Settings -> Tools -> Composer Sync Check`

## Settings Fields

## Composer command

- Field: `Composer command`
- Default: `composer install`
- Purpose: command used for notification-triggered and manual install runs.
- Supports wrappers and containerized commands (for example Docker-based commands).

## Composer file paths

- Fields:
  - `Path to composer.json`
  - `Path to composer.lock`
- Purpose: explicitly define project files used for sync checks.
- Behavior:
  - Accepts absolute or project-relative paths.
  - Includes `Auto-detect` helpers.
  - Validates that paths exist and point to the expected file names.

## Check interval (minutes)

- Field: `Check interval (minutes, 0 disables periodic checks)`
- Purpose: controls periodic background checks.
- `0` disables periodic checks.

## Trigger toggles

- `Check on Git branch change`
  - Runs checks when Git branch/revision changes and `composer.lock` diff indicates relevant changes.
- `Check on composer.json or composer.lock change`
  - Runs checks when either Composer file changes.

## Notification settings

- `Show notification balloon when mismatch is detected`
  - Master switch for all out-of-sync notifications.
- `Test notification balloon`
  - Shows the same out-of-sync notification balloon used by real mismatch detection.

Notification behavior when enabled:
- Manual status checks always show notification on out-of-sync result.
- Test notification action always shows notification.
- Periodic out-of-sync notifications are shown for the first two occurrences per IDE session and skipped from the third onward.
- To hide notifications completely, disable the notifications toggle.

## Debug mode

- Field: `Enable debug mode`
- Purpose: writes detailed check lifecycle logs to the `Composer Sync Check` tool window.

## Tool Window Controls

The `Composer Sync Check` tool window provides:

- `Check composer status now`
  - Runs an immediate sync check.
- `Run composer install now`
  - Runs the configured Composer command immediately.
- Status indicator:
  - Green: in sync
  - Yellow: running check/install
  - Red: out of sync or last run failed

While a status check is running, both action buttons are disabled.

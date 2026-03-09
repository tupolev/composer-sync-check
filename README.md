# Composer Sync Check

IntelliJ Platform plugin that detects when Composer dependencies may be out of sync and prompts you to run `composer install`. Tested primarily in PhpStorm.

<!-- Plugin description -->
**Composer Sync Check** is an IntelliJ Platform plugin (tested in PhpStorm) that detects when your Composer dependencies may be out of sync with the current project state and suggests running `composer install`.

It supports:
- checks on `composer.json` / `composer.lock` changes
- checks on Git repository state changes
- periodic background checks
- configurable composer command execution (including Docker-based commands)
- localized UI messages (English, German, Spanish)
<!-- Plugin description end -->

## Features

- Detects sync mismatches using:
  - `composer.lock` timestamp
  - `vendor/composer/installed.json` timestamp
  - fallback checks for `vendor/`
- Notifications with actions:
  - Run `composer install`
  - Ignore for this session
- Settings under `Settings -> Tools -> Composer Sync Check`:
  - Composer command (supports Docker/wrappers/env vars)
  - Check interval (minutes)
  - Check on Git branch change
  - Check on `composer.json`/`composer.lock` changes
  - Enable/disable warning notifications
- Dedicated `Composer Sync Check` tool window showing command output.

## Build

```bash
./gradlew test buildPlugin
```

Plugin ZIP is generated in `build/distributions`.

## Local Installation

1. Open IntelliJ IDEA or PhpStorm.
2. Go to `Settings -> Plugins`.
3. Click the gear icon and choose `Install Plugin from Disk...`.
4. Select the ZIP from `build/distributions`.

## Development Notes

- Plugin ID: `composer-sync-check`
- Minimum build: `252`
- Bundled dependency: `Git4Idea`

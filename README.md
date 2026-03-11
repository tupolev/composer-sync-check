# Composer Sync Check

IntelliJ Platform plugin that detects when Composer dependencies may be out of sync and prompts you to run `composer install`. Tested primarily in PhpStorm.

[![Build](https://github.com/tupolev/composer-sync-check/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/tupolev/composer-sync-check/actions/workflows/build.yml)

<!-- Plugin description -->
**Composer Sync Check** is an IntelliJ Platform plugin (tested in PhpStorm) that detects when your Composer dependencies may be out of sync with the current project state and suggests running `composer install`.

It supports:
- checks on `composer.json` / `composer.lock` changes
- checks on Git repository state changes
- periodic background checks
- manual on-demand status checks from the tool window
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
- Notification behavior:
  - manual status checks and settings test notifications are always shown when notifications are enabled
  - periodic out-of-sync notifications are shown for the first two occurrences per IDE session, then suppressed for that session
- Settings under `Settings -> Tools -> Composer Sync Check`:
  - Composer command (supports Docker/wrappers/env vars)
  - Check interval (minutes)
  - Check on Git branch change
  - Check on `composer.json`/`composer.lock` changes
  - Enable/disable warning notifications
  - Test notification balloon button
- Dedicated `Composer Sync Check` tool window showing command output with:
  - `Check composer status now` action
  - `Run composer install now` action

## Build

```bash
./gradlew test buildPlugin
```

Plugin ZIP is generated in `build/distributions`.

## Install in PhpStorm

Install from the JetBrains Marketplace:

- <https://plugins.jetbrains.com/plugin/30616-composer-sync-check>

## Local Installation

1. Open IntelliJ IDEA or PhpStorm.
2. Go to `Settings -> Plugins`.
3. Click the gear icon and choose `Install Plugin from Disk...`.
4. Select the ZIP from `build/distributions`.

## Support

- Report issues: <https://github.com/tupolev/composer-sync-check/issues>
- **Support the developer** <a href="https://ko-fi.com/O5O51FHM"><img src="https://storage.ko-fi.com/cdn/kofi6.png?v=6" alt="Buy me a coffee at ko-fi.com" width="180" /></a>

# Composer Sync Check - Agent Guide

## Project Summary
Composer Sync Check is an IntelliJ Platform plugin (primarily for PhpStorm) that detects when PHP Composer dependencies may be out of sync and helps the user run `composer install`.

## Install in PhpStorm
- JetBrains Marketplace: <https://plugins.jetbrains.com/plugin/30616-composer-sync-check>

## Core Behavior
- Detects potential mismatch between project state and installed dependencies.
- Watches `composer.json` / `composer.lock` changes.
- Watches Git repository state changes and reacts to lock-file diffs across revisions.
- Supports periodic background checks.
- Shows actionable notifications when out-of-sync is detected.
- Runs configurable composer commands and streams output to the plugin tool window.

## Main User Surfaces
- Settings page: `Settings -> Tools -> Composer Sync Check`
- Tool window: `Composer Sync Check`
  - Manual check button for sync status (`Check composer status now`)
  - Manual run button for `composer install`
  - Status indicator (green/yellow/red)
  - Console-style output panel with ANSI rendering

## Important Modules
- Settings
  - `src/main/kotlin/net/kodesoft/composersynccheck/settings/ComposerSyncCheckConfigurable.kt`
  - `src/main/kotlin/net/kodesoft/composersynccheck/settings/ComposerSyncCheckSettingsState.kt`
- Core orchestration
  - `src/main/kotlin/net/kodesoft/composersynccheck/services/ComposerSyncProjectService.kt`
- Detection
  - `src/main/kotlin/net/kodesoft/composersynccheck/detection/ComposerSyncDetector.kt`
  - `src/main/kotlin/net/kodesoft/composersynccheck/detection/ComposerFilesLocator.kt`
- Command execution
  - `src/main/kotlin/net/kodesoft/composersynccheck/execution/ComposerCommandRunner.kt`
- UI / tool window
  - `src/main/kotlin/net/kodesoft/composersynccheck/toolwindow/ComposerSyncConsoleService.kt`
  - `src/main/kotlin/net/kodesoft/composersynccheck/toolwindow/ComposerSyncToolWindowFactory.kt`

## Build and Test
- Run tests: `./gradlew test`
- Build plugin zip: `./gradlew buildPlugin`
- Distribution artifact: `build/distributions/`

## Metadata and Resources
- Plugin descriptor: `src/main/resources/META-INF/plugin.xml`
- Message bundles: `src/main/resources/messages/ComposerSyncCheckBundle*.properties`
- Icons: `src/main/resources/META-INF/`

## Notes for Contributors/Agents
- Keep behavior consistent across notification-triggered and manual composer runs.
- Notification policy:
  - Manual status checks and settings test notifications must not be filtered by deduplication or "ignore for session".
  - Periodic out-of-sync notifications are shown only for the first two occurrences per IDE session.
  - Global notifications toggle remains the only way to fully disable notifications.
- Preserve localization by adding message keys in EN/DE/ES bundles together.
- Validate IntelliJ UI changes by running `buildPlugin`.
- If version changes, keep `gradle.properties` and `CHANGELOG.md` aligned.

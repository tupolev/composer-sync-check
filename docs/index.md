# Composer Sync Check

Composer Sync Check is an IntelliJ Platform plugin (tested primarily in PhpStorm) that detects when Composer dependencies may be out of sync with your current project state and helps you run `composer install` quickly.

## What It Does

- Detects potential dependency mismatches using `composer.lock`, `vendor/composer/installed.json`, and `vendor/` fallback checks.
- Triggers checks on:
  - `composer.json` / `composer.lock` changes
  - Git repository state changes
  - periodic background intervals
- Shows actionable notifications when mismatches are detected.
- Runs configurable Composer commands (including wrappers and Docker-based commands).

## Tool Window

The **Composer Sync Check** tool window provides:

- manual **Run composer install now** action
- sync status indicator (green/yellow/red)
- settings shortcut button
- console-like output with ANSI color/format support
- command stdout/stderr streaming and logs

## Settings

Available under:

`Settings -> Tools -> Composer Sync Check`

Configuration includes:

- Composer command
- `composer.json` path
- `composer.lock` path
- check interval (minutes)
- Git change checks
- Composer file change checks
- notifications toggle
- debug mode

Path fields support autodetection and validation.

## Installation

Build plugin ZIP:

```bash
./gradlew buildPlugin
```

Install in IDE:

1. Open your IDE settings
2. Go to `Plugins`
3. Use `Install Plugin from Disk...`
4. Select ZIP from `build/distributions`

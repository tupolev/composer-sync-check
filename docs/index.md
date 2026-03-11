# Composer Sync Check

Composer Sync Check is an IntelliJ Platform plugin (primarily for PhpStorm) that detects when your Composer dependencies may be out of sync with the current project state and helps you run `composer install` quickly.

## Status
[![Build](https://github.com/tupolev/composer-sync-check/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/tupolev/composer-sync-check/actions/workflows/build.yml)


## Main Use Case

Use this plugin when you switch branches, pull lock-file changes, or edit `composer.json` and want a fast warning that dependencies should be reinstalled.

The plugin checks common drift scenarios and provides direct actions to run your configured Composer command.

## Main Features

- Detects mismatches using `composer.lock`, `vendor/composer/installed.json`, and `vendor/` fallback checks.
- Runs checks on:
  - `composer.json` / `composer.lock` changes
  - Git repository state changes
  - periodic background intervals
  - manual on-demand checks from the tool window
- Shows actionable notifications when out-of-sync is detected.
- Provides a dedicated tool window with status indicator, on-demand check/install actions, and ANSI-rendered command output.
- Supports custom Composer commands (including wrappers and Docker-based commands).
- Localized UI (English, German, Spanish).

## Download

- GitHub repository: <https://github.com/tupolev/composer-sync-check>
- Release builds: <https://github.com/tupolev/composer-sync-check/releases>

If you build locally, the plugin ZIP is generated under `build/distributions/`.

## Install in PhpStorm

Install from the JetBrains Marketplace:

- <https://plugins.jetbrains.com/plugin/30616-composer-sync-check>

## Clone and Setup

Prerequisites:
- JDK 21
- Gradle wrapper (included)
- PhpStorm or IntelliJ IDEA compatible with build `252+`

Clone:

```bash
git clone https://github.com/tupolev/composer-sync-check.git
cd composer-sync-check
```

Build and test:

```bash
./gradlew test buildPlugin
```

Run in a sandbox IDE for development:

```bash
./gradlew runIde
```

Install built ZIP manually:

1. Open IDE settings.
2. Go to `Plugins`.
3. Select `Install Plugin from Disk...`.
4. Choose the ZIP from `build/distributions/`.

## Configuration

Plugin configuration details are documented in [Configuration](configuration.md).

## Support

- Report issues: <https://github.com/tupolev/composer-sync-check/issues>
- **Support the developer** <a href="https://ko-fi.com/O5O51FHM"><img src="https://storage.ko-fi.com/cdn/kofi6.png?v=6" alt="Buy me a coffee at ko-fi.com" width="180" /></a>

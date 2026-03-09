<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Composer Sync Check Changelog

## [0.1.0] - 2026-03-09

### Added

- Initial release of Composer Sync Check.
- Composer sync detection based on:
  - `composer.lock` vs installed dependencies timestamps
  - `vendor/composer/installed.json` availability and freshness
  - `vendor/` fallback checks when needed
- Multiple check triggers:
  - debounced checks on `composer.json` / `composer.lock` changes
  - checks on Git repository state changes (including lock-file diff detection)
  - periodic background checks with configurable interval
- Actionable mismatch notification with:
  - `Run composer install`
  - `Ignore for this session`
- Fully configurable command execution:
  - custom composer command string
  - support for wrappers, Docker-based commands, and env-driven commands
  - configurable `composer.json` and `composer.lock` paths (relative or absolute)
  - autodetect actions for both composer files in settings
- Dedicated `Composer Sync Check` tool window:
  - manual `Run composer install now` action
  - status indicator (in sync / out of sync / running)
  - settings shortcut button
  - console-style output with ANSI color/format support
  - stdout/stderr streaming and command lifecycle logs
- Command result feedback:
  - success/failure balloon notifications
  - soft green success balloon and soft red failure balloon styles
- Settings UX improvements:
  - sectioned checkbox group with helper descriptions
  - path field validation outlines (green/red) on blur and autodetect
  - debug mode option for detailed check logging in the tool window
- Localization support for English, German, and Spanish.

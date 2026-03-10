<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Composer Sync Check Changelog

## [Unreleased]

## [0.1.5] - 2026-03-10

### Added

- Settings page action to test the out-of-sync notification balloon.
- Tool window action `Check composer status now` (loupe icon) for manual on-demand sync checks.

### Changed

- Out-of-sync notifications now use the plugin tool window icon.
- Tool window header now shows only status/actions (plugin title removed from header row).
- Notification behavior refined:
  - manual status checks and settings test notifications are always shown when notifications are enabled
  - periodic out-of-sync notifications are shown for the first two occurrences per IDE session and skipped from the third onward
- Documentation updates (`README.md`, `docs/index.md`, `docs/configuration.md`, `AGENTS.md`).
- Added notification policy unit tests.

## [0.1.4] - 2026-03-09

### Changed

- Internal maintenance release and workflow/settings stabilization updates.

## [0.1.3] - 2026-03-09

### Added

- Manual `Run composer install now` action in the `Composer Sync Check` tool window.
- Header status indicator (traffic-light style):
  - green = in sync
  - yellow = composer command running
  - red = out of sync or last composer run failed
- Tool window settings shortcut button (gear) to open plugin settings directly.
- Console-style tool window output with:
  - ANSI color/format rendering
  - black background and monospace text
  - context menu actions (cut/copy/paste/select all/search selection in Google)
- Command lifecycle UX:
  - run button disables while command is running and re-enables afterward
  - success/failure balloons for composer command results
  - soft green success balloon and soft red failure balloon styles
- Settings improvements:
  - debug mode option
  - checkbox option grouping with section separator
  - helper description text below checkboxes
  - path field red/green outline validation on blur and autodetect
- Tool window custom icon.
- `docs/index.md` page for GitHub Pages.

### Changed

- Release workflow now uploads plugin ZIP assets to GitHub Releases.
- Release workflow validates tag/version consistency and SemVer format.
- JetBrains Marketplace publishing is temporarily disabled in release workflow (kept ready for re-enable).
- Removed Qodana from Gradle and CI workflow.

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
- Configurable command execution and composer file path configuration.
- Localization support for English, German, and Spanish.

[Unreleased]: https://github.com/tupolev/composer-sync-check/compare/0.1.5...HEAD
[0.1.5]: https://github.com/tupolev/composer-sync-check/compare/0.1.4...0.1.5
[0.1.4]: https://github.com/tupolev/composer-sync-check/compare/0.1.3...0.1.4
[0.1.3]: https://github.com/tupolev/composer-sync-check/compare/0.1.0...0.1.3
[0.1.0]: https://github.com/tupolev/composer-sync-check/commits/0.1.0

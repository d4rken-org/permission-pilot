# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About Permission Pilot

Permission Pilot is an Android app that helps users understand and manage app permissions. It provides detailed views of which apps request which permissions, tracks permission changes over time, and offers filtering and sorting tools.

## Build Flavors

- **foss**: Open-source version for F-Droid/GitHub releases
- **gplay**: Google Play version with billing client for in-app purchases

## Build Types

- **debug**: Unobfuscated, full logging, no minification
- **beta**: Production-ready with strict lint checks
- **release**: Fully optimized for production distribution

## Rules

Always loaded (`.claude/rules/`):
- `architecture.md` — Module structure, patterns, base classes, data flow
- `code-style.md` — Kotlin conventions, ViewModel/Compose patterns, logging
- `commit-guidelines.md` — Commit message format, PR description format, area prefixes
- `build-commands.md` — Build, test, and lint commands

Loaded only when touching matching files (`paths:` frontmatter):
- `testing.md` — Test locations and patterns. Triggers on `app/src/{test,testGplay,testShared,androidTest,screenshotTest*}/`
- `localization.md` — String resource conventions. Triggers on `app/src/*/res/values*/` and `fastlane/metadata/`
- `screenshots.md`, `release.md` — one-line pointers that tell you to invoke the matching skill

## Skills

Multi-step procedures in `.claude/skills/`. The body loads only when invoked:
- `/screenshots` — Play Store screenshot regeneration, en-US-only git tracking, and upload
- `/release` — Version bumping and release tagging

Read `testing.md` before deciding whether or how to add tests — a path trigger only fires once a matching file is actually read, so it won't have loaded while you're still deciding.

**Do not add `paths:` to a skill.** On a skill it is restrictive, not additive: it gates the skill off entirely until a matching file is read, so `/screenshots` and `/release` become uninvocable by name. Path triggering for these two is handled by the pointer rules above instead.

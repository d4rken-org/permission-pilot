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

Detailed guidelines are in `.claude/rules/`:
- `commit-guidelines.md` — Commit message format, PR description format, area prefixes
- `build-commands.md` — Build, test, lint, screenshot, and release commands
- `screenshots.md` — Play Store screenshot tracking, regeneration, and upload workflow
- `architecture.md` — Module structure, patterns, base classes, data flow
- `code-style.md` — Kotlin conventions, ViewModel/Compose patterns, logging
- `testing.md` — Test locations, patterns, running tests
- `localization.md` — String resources, naming conventions

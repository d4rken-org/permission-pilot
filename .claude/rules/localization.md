---
paths:
  - "app/src/*/res/values*/**"
  - "fastlane/metadata/**"
---

# Localization Guidelines

## String Extraction

- All user-facing texts must use string resources — never hardcode user-facing text
- Before creating a new entry, check if `strings.xml` already contains a suitable general string

## String ID Naming

- Follow naming convention: `feature_component_description` (e.g., `permissions_filter_label`)
- Re-used strings should be prefixed with `general` or `common`

## String Format Conventions

- Localized strings with multiple arguments should use ordered placeholders: `%1$s is %2$d`
- Use ellipsis characters (`…`) instead of 3 manual dots (`...`)

## Resource Locations

- `app/src/main/res/values/strings.xml`: Base English strings
- `app/src/main/res/values-*/strings.xml`: Translated strings for other languages

## Translator Context

String context, character limits and file context are managed on Crowdin through the android-translation plugin's `crowdin-annotate` skill. XML comments in `values/strings.xml` no longer reach translators once a string's context has been written on Crowdin; change it there.

# Release

Operator-facing release docs: see `.github/RELEASING.md`.

## AI-relevant facts

- `tools/release/bump.sh` is the single source of truth for version logic. Its versionCode formula must stay in sync with `buildSrc/src/main/java/ProjectConfig.kt`.
- CI calls `bash tools/release/bump.sh --mode=check` on every PR through the `check-release-tooling` job in `code-checks.yml`. If you touch `version.properties` or `VERSION` by hand, run that command locally before committing.
- `release-prepare.yml` is the only sanctioned path for bumping versions and creating release tags. Do not suggest editing `version.properties` directly, creating tags manually, or running the old `release.sh`.
- `validate-tag` in `release-tag.yml` delegates to `bump.sh --mode=check --expected-tag=<tag>`. Both tag format and version file consistency are validated inside the script, not in YAML.

# Releasing Permission Pilot

## How to cut a release

1. Go to **Actions > Release prepare > Run workflow**.
2. Pick:
   - **bump_kind**: `build` for a follow-up build, `patch` for a bug fix, `minor` for a feature, `major` for a breaking change.
   - **version_type**: `keep-current` for the usual path, or `rc` / `beta` to switch the release channel.
   - **dry_run**: `true` first to preview the plan; `false` to commit, tag, and push.
3. With `dry_run=false`, Job 1 computes and validates. Job 2 commits the bump, creates an annotated tag, and pushes both atomically to `main`.
4. The App-token push fires `release-tag.yml` automatically. Do not manually dispatch the tag workflow for a real cut.
5. Cancel window: between Job 1 and Job 2 there are only a few seconds to cancel from the Actions run page if the summary looks wrong.

## Version scheme

Defined in `tools/release/bump.sh`. All fields must be in `0..99` because the formula overlaps at 100:

```text
versionName = <major>.<minor>.<patch>-<type><build>   (example: 2.1.0-rc0)
versionCode = major*10_000_000 + minor*100_000 + patch*1_000 + build*10
```

Files updated by every release:
- `version.properties`, read by Gradle at build time.
- `VERSION`, plain text for third-party consumers.

## Tag to channel mapping

| Tag suffix | FOSS build | FOSS GitHub release | Gplay lane | Play Store track | Rollout |
|---|---|---|---|---|---|
| `-beta<n>` | `assembleFossBeta` | pre-release | `lane :beta` | `beta` | no staged rollout configured |
| `-rc<n>` | `assembleFossRelease` | release | `lane :production` | `beta` | no staged rollout configured |

`lane :production` uploads to Play's `beta` track in this repo. Manual promotion to production is done outside this release flow. `lane :listing_only` is the only Fastlane path that targets Play's `production` track, and it skips APK/AAB upload.

## Validation guards

- `check-release-tooling` in `code-checks.yml` runs `shellcheck`, `bats`, and a live `--mode=check` on every PR.
- `validate-tag` in `release-tag.yml` runs `bump.sh --mode=check --expected-tag=<tag>` on every tag push. It rejects malformed tags and tags that do not match `version.properties`.
- Job 2 of `release-prepare.yml` calls `--mode=check --expected-current=<name-from-job-1>` before writing, so it fails if `main` moved between jobs.

## Emergency release

The branch ruleset bypass should be configured for the `d4rken-org-releaser` GitHub App. A human direct push should be rejected. If Actions are unavailable:

1. An org admin temporarily adds the relevant human account as a bypass actor on the main-branch ruleset.
2. Run locally: `bash tools/release/bump.sh --mode=write --bump-kind=<kind>`, then commit, tag, and push manually.
3. Remove the human bypass actor immediately after.

## First-time repo setup

Required before `release-prepare.yml` can push:

1. Install `d4rken-org-releaser` on this repo.
2. Ensure org secrets `RELEASE_APP_CLIENT_ID` and `RELEASE_APP_PRIVATE_KEY` are scoped to this repo.
3. Add the App as bypass actor in:
   - the `main` branch ruleset for PR and status-check requirements.
   - any tag ruleset restricting `v*` creation.

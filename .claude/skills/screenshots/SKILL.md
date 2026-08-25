---
name: screenshots
description: Play Store screenshot pipeline — regenerate localized screenshots, copy them into fastlane metadata, and upload to Play Store. Covers the en-US-only git tracking and the post-upload working-tree restore.
when_to_use: When regenerating, refreshing, or uploading Play Store screenshots, when the en-US PNGs need re-rendering after a UI change, or when working in fastlane/ or app/src/screenshotTest*/.
effort: low
---

# Play Store Screenshots

## What's tracked in git

- **en-US only (6 PNGs)** — `fastlane/metadata/android/en-US/images/phoneScreenshots/`. This is the set the README gallery links to.
- **The other 38 locales** — gitignored. Generated on demand for upload, not committed.

The `.gitignore` rule (lines following `app/src/screenshotTest*/reference/`) ignores all phone screenshots and `!`-includes only `en-US`.

`generate_screenshots.sh --smoke` renders 6 locales (`en-US`, `de-DE`, `ja-JP`, `ar`, `zh-CN`, `pt-BR`) covering LTR, RTL, and CJK. It is a rendering-iteration aid for checking layout under different scripts — it has nothing to do with what git tracks. Of its output, only the `en-US` set is committable; the other five land as ignored files.

## Why en-US only

- **Reviewable diffs** — a screenshot refresh that touches 6 PNGs is reviewable; 234 PNGs is not.
- **Repo size** — keeps binary churn out of git history.
- **Fastlane behavior** — `supply` skips locales with no local screenshot files, so unpushed locales keep whatever Play Store currently has. This is current Fastlane uploader behavior, not a Play Store guarantee.

## Full regeneration + upload (on demand)

1. **Generate** all 39 locales:
   ```bash
   ./fastlane/generate_screenshots.sh
   ```
   Batched gradle screenshot rendering (~20 batches, ~10–15 min). The script exits non-zero if the final PNG count differs from the expected 234 (39 × 6).

2. **Copy** rendered PNGs into fastlane metadata dirs:
   ```bash
   ./fastlane/copy_screenshots.sh --clean
   ```
   Output: `fastlane/metadata/android/<locale>/images/phoneScreenshots/*.png` for all 39 locales. Exits non-zero on an unknown composable name or an incomplete locale.

3. **Verify** count before upload:
   ```bash
   find fastlane/metadata/android -path "*/images/phoneScreenshots/*.png" | wc -l   # expect 234
   ```

4. **Upload** to Play Store:
   ```bash
   cd fastlane && bundle exec fastlane screenshots_only
   ```
   The lane invokes `remove_unsupported_languages.sh` first, which deletes `ckb-IR` and `ku-TR` translation dirs (and harmlessly errors on the other 14 absent dirs in its list).

5. **Restore** working tree post-upload:
   ```bash
   git clean -fdX fastlane/metadata/android      # removes only ignored files (the 38 non-en-US screenshot sets)
   git checkout -- fastlane/metadata/android/ckb-IR fastlane/metadata/android/ku-TR
   ```
   `git clean -fdX` removes only gitignored files, so tracked screenshots, translations, and listing assets are untouched. The `git checkout` restores the two translation dirs the lane deleted.

## After the first en-US-only upload

Spot-check at least one **non-en-US** locale (e.g. `fr-FR`) in Play Console → Store listing → that locale, and confirm screenshots are still present. This validates the "Fastlane skips locales with no local files" assumption against current Play Store behavior. If other locales lose their screenshots, the workflow needs adjustment (e.g. always upload the full 39 set, or revert the gitignore split).

## When screenshots change

Re-render across scripts while iterating, then copy:
```bash
./fastlane/generate_screenshots.sh --smoke
./fastlane/copy_screenshots.sh --clean
```
Only the `en-US` set ends up in the commit (typically `Apps:` / `Permissions:` / `General:` depending on what UI changed); the other five renders stay ignored and can be dropped with `git clean -fdX fastlane/metadata/android`.

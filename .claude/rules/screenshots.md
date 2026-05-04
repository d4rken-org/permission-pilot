# Play Store Screenshots

## What's tracked in git

- **Smoke locales (6)** — `en-US`, `de-DE`, `ja-JP`, `ar`, `zh-CN`, `pt-BR`. PNGs under `fastlane/metadata/android/<locale>/images/phoneScreenshots/` are committed for these locales only.
- **Other 33 locales** — gitignored. Generated on demand for upload, not committed.

The `.gitignore` rule (lines following `app/src/screenshotTest*/reference/`) ignores all phone screenshots and `!`-includes only the 6 smoke locales. The smoke set matches `SMOKE_LOCALES` in `fastlane/generate_screenshots.sh`.

The `--smoke` mode of `generate_screenshots.sh` is also useful for fast UI iteration on representative LTR/RTL/CJK locales without rendering all 39.

## Why smoke-only

- **Reviewable diffs** — a screenshot refresh that only touches 6 × 6 = 36 PNGs is reviewable; 234 PNGs is not.
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
   Output: `fastlane/metadata/android/<locale>/images/phoneScreenshots/*.png` for all 39 locales.

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
   git clean -fdX fastlane/metadata/android      # removes only ignored files (the 33 non-smoke screenshot sets)
   git checkout -- fastlane/metadata/android/ckb-IR fastlane/metadata/android/ku-TR
   ```
   `git clean -fdX` removes only gitignored files, so tracked screenshots, translations, and listing assets are untouched. The `git checkout` restores the two translation dirs the lane deleted.

## After the first smoke-only upload

Spot-check at least one **non-smoke** locale (e.g. `fr-FR`) in Play Console → Store listing → that locale, and confirm screenshots are still present. This validates the "Fastlane skips locales with no local files" assumption against current Play Store behavior. If non-smoke locales lose their screenshots, the workflow needs adjustment (e.g. always upload the full 39 set, or revert the gitignore split).

## When smoke screenshots change

Re-rendering the 6 smoke locales is part of the normal `--smoke` test loop:
```bash
./fastlane/generate_screenshots.sh --smoke
./fastlane/copy_screenshots.sh --clean
```
Resulting changes to the 6 smoke locale PNGs go into a normal commit (typically `Apps:` / `Permissions:` / `General:` depending on what UI changed).

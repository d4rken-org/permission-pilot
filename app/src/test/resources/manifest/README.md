# Binary AXML fixtures

Real `AndroidManifest.xml` blobs produced by `aapt2 link`, used by
`AapFixtureIntegrationTest` to cross-validate the streaming binary-XML parser against
output from the actual Android build toolchain.

These complement — not replace — the synthetic `AxmlFixtureBuilder` in
`app/src/test/java/testhelper/binaryxml/`. The synthetic builder covers malformed cases
deterministically; these real blobs guard against the risk that the builder and the
parser share the same mental model of the format.

## Provenance

| Blob | Source XML | Exercises |
|---|---|---|
| `simple.manifest.bin` | `app/src/test/fixtures-src/manifest/simple.xml` | Baseline: `<uses-permission>`, `<uses-sdk>`, `<activity>` |
| `refs.manifest.bin` | `app/src/test/fixtures-src/manifest/refs.xml` | Framework resource references (`@android:drawable/...`, `@android:style/...`) |
| `queries.manifest.bin` | `app/src/test/fixtures-src/manifest/queries.xml` | `<queries>` block with `<package>`, `<intent>` (action+data+category), `<provider>` |
| `enum_flags.manifest.bin` | `app/src/test/fixtures-src/manifest/enum_flags.xml` | `android:protectionLevel`, `launchMode`, `configChanges`, `screenOrientation`, `windowSoftInputMode` — framework flag/enum attributes that the renderer resolves via `ManifestEnumFlagNames` |
| `nulls.manifest.bin` | `app/src/test/fixtures-src/manifest/nulls.xml` | `android:taskAffinity=""` — tests `TYPE_STRING` with empty values and `TYPE_NULL` handling |

## Regenerating

Run `bash app/src/test/fixtures-src/generate.sh` from any shell with `local.properties` pointing
at an Android SDK that has a `platforms/android-36/android.jar` (or any recent platform). The
script picks `aapt2` from the Gradle transforms cache automatically — run any Gradle task first
if the cache is empty.

The script is idempotent and rewrites all blobs in this directory. Commit the result.

## UTF-16 string pool coverage

aapt2 always emits UTF-8 string pools; there is no `--utf16` flag. The UTF-16 decoder path in
`BinaryXmlStringPool` is exercised entirely by the synthetic `AxmlFixtureBuilder` in the unit
tests instead.

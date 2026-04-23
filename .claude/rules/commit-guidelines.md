# Commit & Pull Request Guidelines

## Commit Message Format

```
<area>: <title>

<detailed technical description>

<optional additional context>

<issue references>
```

## Area Prefixes

Use these prefixes to categorize commits and PR titles:
- **Apps**: App listing, filtering, sorting, app details
- **Permissions**: Permission listing, details, permission data
- **Watcher**: Permission change monitoring, snapshots, diffs
- **Overview**: Overview/dashboard screen
- **Settings**: Settings and preferences
- **General**: Cross-cutting concerns, architecture, build system, CI
- **Release**: Version bumps and release candidates (e.g., `Release: 2.0.3-rc0`)
- **Fix**: Bug fixes that don't fit a specific area

## Commit Title Guidelines

Commit titles are for **developers** reading `git log`. They can be technical and reference internal names.

- **Be clear and descriptive**: Describe what was actually changed in the code
- **Use action words**: "Fix", "Add", "Improve", "Update", "Remove", "Refactor"
- **Technical references are fine**: Class names, method names, and implementation details are acceptable

### Commit Examples

```
Apps: Add device admin detection and filter

Detect device admin receivers via DevicePolicyManager, inject synthetic
BIND_DEVICE_ADMIN permission entries, and add filter chip to apps list.

Closes #84
```

```
Fix: Use requestedPermissions for device admin filter

The receiver-only check missed apps that declare BIND_DEVICE_ADMIN
via <uses-permission> without having a device admin receiver.
```

## Pull Request Titles

PR titles use the same area prefixes as commits.

## Pull Request Description Format

### What changed

User-friendly explanation of what this PR does. Describe the problem that was fixed or the feature that was added from
the user's perspective. No internal class or method names.

For non-user-facing PRs (refactors, tests, CI, dependency bumps): write "No user-facing behavior change" followed by a
brief internal description.

### Technical Context

Explain what's hard to extract from the diff alone. Focus on:

- **Why** this approach was chosen (and alternatives considered/rejected)
- **Root cause** for bug fixes (the diff shows the fix, not what caused it)
- **Non-obvious side effects** or behavioral changes not apparent from reading the code
- **Review guidance** — what's tricky or deserves close attention

Keep it scannable with bullet points. Don't restate what's visible in the diff (file names, class renames, line-level
changes).

### Example

```markdown
## What changed

Apps with device admin privileges are now detected and displayed. A new "Device admin"
filter chip is available in the apps list, and the permission details screen shows
granted/denied status for BIND_DEVICE_ADMIN.

## Technical Context

- Follows the existing AccessibilityService pattern: scan receivers, inject synthetic UsesPermission entries
- Active admin list is pre-fetched once per scan (not per-package) for efficiency
- Filter checks requestedPermissions (not just receivers) to also catch apps declaring BIND_DEVICE_ADMIN via manifest
```

## Conventions

- **Issue references**: Use "Closes #123", "Fixes #123", or "Resolves #123"
- **Breaking changes**: Mark with "BREAKING:" prefix if applicable

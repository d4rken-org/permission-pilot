<img src="https://github.com/d4rken-org/permission-pilot/raw/main/.github/assets/app_banner.png" width="400">

# Permission Pilot

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Localized](https://badges.crowdin.net/permission-pilot/localized.svg)](https://crowdin.com/project/permission-pilot)
[![Code tests](https://github.com/d4rken-org/permission-pilot/actions/workflows/code-checks.yml/badge.svg)](https://github.com/d4rken-org/permission-pilot/actions/workflows/code-checks.yml)
[![GitHub Downloads](https://img.shields.io/github/downloads/d4rken-org/permission-pilot/total?label=GitHub%20Downloads&logo=github)](https://github.com/d4rken-org/permission-pilot/releases)
[![Google Play Downloads](https://img.shields.io/endpoint?color=green&logo=google-play&logoColor=green&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Deu.darken.myperm%26l%3DGoogle%2520Play%26m%3D%24totalinstalls)](https://play.google.com/store/apps/details?id=eu.darken.myperm)
[![⭐](https://img.shields.io/endpoint?url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Deu.darken.myperm%26gl%3DUS%26hl%3Den%26l%3D%25E2%25AD%2590%26m%3D%24rating)](https://play.google.com/store/apps/details?id=eu.darken.myperm)
[![RB Status](https://shields.rbtlog.dev/simple/eu.darken.myperm)](https://shields.rbtlog.dev/eu.darken.myperm)

A new kind of app to help the user view apps and which permissions they use.

Born out of a feature discussion for SD Maid. [Origin Story](https://github.com/d4rken-org/permission-pilot/issues/1)

## Introduction

#### THE PROBLEM

This app was created to fill in a void that currently exists in the Android App Market. Android has app permissions scattered in multiple places, requiring the user to navigate through multiple pages within System Settings to properly configure permissions for installed apps. Moreover, there is no way to view the status of all permissions for any app from one place.


#### THE SOLUTION

Permission Pilot gives you a bird's eye view of all permissions for installed apps from one place. Here's what you get:

1. **Overview Dashboard** — A summary of your device showing app counts by category (privacy, security, install source, system). Each category navigates directly to a filtered app list.

2. **Apps tab** — All installed apps, including system apps, work profile, and multi-user apps. Tap any app to see every permission it has requested, their granted/denied status, install source, and a full manifest viewer.

3. **Permissions tab** — All permissions on your device, grouped by category (Contacts, Camera, Microphone, etc.). Tap a permission to see every app that requests it.

4. **Permission Watcher** — Monitors apps for permission changes when they are installed, updated, or removed. Creates reports showing added/removed permissions and grant changes, with optional notifications.

5. **Data Export** — Export app or permission data in Markdown, CSV, or JSON with configurable detail level.

6. **Search, Sort & Filter** — Free-text search, multiple sort options, and filter chips across all views.

#### LIMITATIONS

Android does not allow third-party apps to change permissions on non-rooted devices. Permission Pilot shows all the information in one place and can launch the appropriate System Settings page for any changes you want to make.


## Downloads
| Source                | Status |
|-----------------------|--------|
| [Google Play](https://play.google.com/store/apps/details?id=eu.darken.myperm) | [![](https://img.shields.io/endpoint?color=green&logo=google-play&logoColor=green&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Deu.darken.myperm%26l%3DGoogle%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=eu.darken.myperm) |
| [Google Play Beta](https://play.google.com/apps/testing/eu.darken.myperm)     | [![](https://img.shields.io/badge/Google%20Play-Beta-yellowgreen?style=flat&logo=google-play)](https://play.google.com/apps/testing/eu.darken.myperm)                                                                                                                                   |                                                                                                                  |
| [Github Releases](https://github.com/d4rken-org/permission-pilot/releases/latest) | [![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/d4rken-org/permission-pilot?include_prereleases&label=GitHub)](https://github.com/d4rken-org/permission-pilot/releases/latest) |
| [F-Droid (IzzyOnDroid)](https://apt.izzysoft.de/packages/eu.darken.myperm/) | [![](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/eu.darken.myperm)](https://apt.izzysoft.de/packages/eu.darken.myperm/) |

## Community

Want to chat? Join our [discord server here](https://discord.gg/7gGWxfM5yv).

## Support the project

If you like the app, consider:

* Buying the upgrade on [Google Play](https://play.google.com/store/apps/details?id=eu.darken.myperm)
* [Sponsoring the project](https://github.com/sponsors/d4rken)
* [Contributing translations](https://crowdin.com/project/permission-pilot)

## Screenshots

<img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot1.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot2.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot3.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot4.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot5.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot6.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot7.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot8.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot9.png" width="100"> <img src="https://github.com/d4rken-org/permission-pilot/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot10.png" width="100"> 

## License

Permission Pilot's code is available under a GPL v3 license, this excludes:

* Icons, logos, mascots and marketing materials/assets.

## Thanks

* Thanks to [crowdin.com](https://crowdin.com/) for supporting open-source projects.

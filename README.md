<img src="https://github.com/d4rken-org/permission-pilot/raw/main/.github/assets/app_banner.png" width="400">

# Permission Pilot

[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/d4rken-org/permission-pilot?include_prereleases)](https://github.com/d4rken-org/permission-pilot/releases/latest)
[![Github All Releases](https://img.shields.io/github/downloads/d4rken-org/permission-pilot/total.svg)]()
[![Code tests & eval](https://github.com/d4rken-org/permission-pilot/actions/workflows/code-checks.yml/badge.svg)](https://github.com/d4rken-org/permission-pilot/actions/workflows/code-checks.yml)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)

A new kind of app to help the user view apps and which permissions they use.

Born out of a feature discussion for SD Maid. [Origin Story](https://github.com/d4rken-org/permission-pilot/issues/1)

## Introduction

#### THE PROBLEM

This app was created to fill in a void that currently exists in the Android App Market. Android has app permissions scattered in multiple places, requiring the user to navigate through multiple pages within System Settings to properly configure permissions for installed apps. Moreover, there is no way to view the status of all permissions for any app from one place.


#### THE SOLUTION

Permission Pilot gives the user a bird's eye view of all permissions for installed apps from one place. Here's what you get:

1. View snapshot of ALL permissions that every app has requested, their allowed/ denied status, battery management status, internet access, SharedUserID status, etc. You can see EVERYTHING about each installed app on your device from one place. This includes all hidden permissions too which are not visible under System Settings.

2. Each icon you see is a 'clickable' shortcut that can quickly launch a configurable permission within System Settings, take you directly to the App Info page within System Settings, or take you to the App page on the Store where the app was downloaded/ updated from. This allows the user to quickly view and configure all permissions for each app, without the need to navigate through multiple pages within System Settings.

3. The Permissions Bar shows the most important permissions at a glance, and clicking any of the icons on it will display a short description of the permission at the bottom of your screen, with a link to view the list of all apps that have requested that permission.

4. Several Sort and Filter options have been provided that can be used to quickly view list of apps that satisfy your chosen criteria.

5. Permission Pilot has support for apps installed in Work Profile and Multi-Users, though some of that is still Work-in-Progress (WIP).
 

#### LIMITATIONS

Android does NOT allow for configuring System Settings or App Permissions using a 3rd party app (on non-rooted devices). Permission Pilot, as such, can only show you all the information in one place, and also quickly launch the appropriate page within System Settings for any changes you may want to make to app permissions.


## Downloads
| Source                | Status |
|-----------------------|--------|
| [Google Play](https://play.google.com/store/apps/details?id=eu.darken.myperm) | [![](https://img.shields.io/endpoint?color=green&logo=google-play&logoColor=green&url=https%3A%2F%2Fplayshields.herokuapp.com%2Fplay%3Fi%3Deu.darken.myperm%26l%3DAndroid%26m%3D%24version)](https://play.google.com/store/apps/details?id=eu.darken.myperm) |
| [Google Play Beta](https://play.google.com/apps/testing/eu.darken.myperm) | ? |
| [Github Releases](https://github.com/d4rken-org/permission-pilot/release) | [![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/d4rken-org/permission-pilot?include_prereleases&label=GitHub)](https://github.com/d4rken-org/permission-pilot/releases/latest) |
| [F-Droid (IzzyOnDroid)](https://apt.izzysoft.de/packages/eu.darken.myperm/) | [![](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/eu.darken.myperm)](https://apt.izzysoft.de/packages/eu.darken.myperm/) |

## Community

Want to chat? Join our [discord server here](https://discord.gg/7gGWxfM5yv).

## Support the project

If you like the app, consider:

* Contributing translations
* Contributing features or bugfixes
* Buying the upgrade on [Google Play](https://play.google.com/store/apps/details?id=eu.darken.myperm)
* [Sponsoring the project](https://github.com/sponsors/d4rken)

## Screenshots

<img src="https://raw.githubusercontent.com/d4rken-org/permission-pilot/fastlane/metadata/android/en-US/images/screenshot1.png" width="128">
<img src="https://raw.githubusercontent.com/d4rken-org/permission-pilot/fastlane/metadata/android/en-US/images/screenshot2.png" width="128">
<img src="https://raw.githubusercontent.com/d4rken-org/permission-pilot/fastlane/metadata/android/en-US/images/screenshot3.png" width="128">
<img src="https://raw.githubusercontent.com/d4rken-org/permission-pilot/fastlane/metadata/android/en-US/images/screenshot4.png" width="128">
<img src="https://raw.githubusercontent.com/d4rken-org/permission-pilot/fastlane/metadata/android/en-US/images/screenshot5.png" width="128">
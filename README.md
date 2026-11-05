# Hydr8M8

*HydrateMate* – a small Android app for tracking daily water intake, built for two people on Galaxy S24 phones. Not on the Play Store; releases are signed APKs attached to GitHub Releases, and the app can update itself from there.

## Features

- **One-tap logging** of preset amounts (European glass/bottle sizes, each with its own icon) or a custom amount in 10 ml steps
- **Daily goal** with history: changing the goal never rewrites past days
- **Alcohol** raises the day's goal (10 ml per gram of alcohol) instead of counting as water
- **Weather bonus**: extra water on warm/hot days (approximate location + Open-Meteo)
- **Calendar** with % of goal per day; tap a day to back-fill or edit entries
- **Streak** with jokers (one per 7 successful days, max 3) and milestone notifications
- **Reminders** after a configurable time without drinking, respecting wake/sleep windows and Do Not Disturb; reminder buttons log directly
- **Live progress** in the notification shade / Now Bar (Android 16+)
- **Home-screen widget** (resizable, configurable per instance) and **launcher shortcuts** (long-press the app icon)
- **Drop Mate** mascot whose mood follows your pace; subtle sound effects
- English and German UI and light/dark theme, each following the system or overridden in the settings
- **In-app updates**: checks GitHub Releases on start (switchable) or on demand, downloads with a progress bar and installs
- Android Auto Backup

## Install

Download the latest `Hydr8M8-<version>.apk` from [Releases](../../releases) and open it on the phone (allow installing from this source once). Later updates can be installed from within the app (Settings → Updates); Android asks once to allow Hydr8M8 to install apps.

## Development

Requires Android Studio's JDK (JDK 17+ with `javac`) and the Android SDK (API 37).

```sh
./gradlew testDebugUnitTest spotlessCheck lintDebug assembleDebug
```

## License

[PolyForm Noncommercial 1.0.0](LICENSE.md): you may use, study, change and share Hydr8M8 for any **non-commercial** purpose (personal use, hobby projects, research, education, charitable organisations). Any commercial use, including selling the app or a fork, is not permitted. Versions published before 2026-11-06 were released under the MIT License.

Found a security issue? See [SECURITY.md](SECURITY.md).

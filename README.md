# Hydr8M8

*HydrateMate* – an Android app (Android 14+) for tracking your daily water intake. It is not on the Play Store; releases are signed APKs on GitHub Releases, and the app checks for updates itself.

## Features

- **One-tap logging** of preset amounts (European glass/bottle sizes, each with its own icon) or a custom amount in 10 ml steps
- **Daily goal**, set manually or calculated from sex, age, height, weight and build (DGE/EFSA reference values); changing it never rewrites past days
- **Alcohol** raises the day's goal (10 ml per gram of alcohol) instead of counting as water
- **Weather bonus**: extra water on warm/hot days (approximate location + Open-Meteo)
- **Calendar** with % of goal per day; tap a day to back-fill or edit entries
- **Streak** with jokers (one per 7 successful days, max 3) and milestone notifications
- **Reminders** after a configurable time without drinking, respecting wake/sleep windows and Do Not Disturb; reminder buttons log directly
- **Live progress** in the status bar and, optionally, on the lock screen (Live Update on Android 16+)
- **Home-screen widget** (resizable, configurable per instance) and **launcher shortcuts** (long-press the app icon)
- **Custom amounts** with your own name and icon
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

[PolyForm Noncommercial 1.0.0](LICENSE.md): you may use, study, change and share Hydr8M8 for any **non-commercial** purpose (personal use, hobby projects, research, education, charitable organisations). Any commercial use, including selling the app or a fork, is not permitted.

Found a security issue? See [SECURITY.md](SECURITY.md).

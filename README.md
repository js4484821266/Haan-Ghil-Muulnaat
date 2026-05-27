# Haan Ghil Muulnaat / 한 길 물낯

Haan Ghil Muulnaat is an Android app for local portrait perturbation experiments and post-restoration defense evaluation.

한국어 표시명은 **한 길 물낯**이고, 다른 언어의 표시명은 **Haan Ghil Muulnaat**입니다.

## What It Does

- Loads portrait images from the Android gallery or Android share sheet.
- Applies local image perturbation at a selected strength.
- Searches for a minimum viable perturbation strength with binary search.
- Runs a restoration-style evaluation pass after protection.
- Reports practical outcomes as `PASS`, `HELD`, or `BROKEN`.
- Saves protected images back to the user's gallery when requested.

## Privacy Model

The app is designed for on-device processing. It does not implement an app-level image upload path, and selected images are processed locally.

See [PRIVACY.md](PRIVACY.md) for details about image handling, Android permissions, and Google Play Services ML Kit model behavior.

## Evaluation Scope

This project is a practical diagnostic tool, not a universal security guarantee.

- `PASS`: the baseline protected image disrupts the configured local probe.
- `HELD`: protection still holds after the app's restoration-style evaluation pass.
- `BROKEN`: the evaluation recovers enough signal that stronger tuning or another method is needed.

Results depend on the device, image, model behavior, and attack assumptions. Do not treat a local `HELD` result as proof of protection against every classifier or restoration pipeline.

## Build

Requirements:

- Android Studio or Android SDK
- JDK 17+
- Windows PowerShell for the helper scripts, or Gradle directly on any supported platform

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Build a local release APK:

```powershell
.\build-android.ps1
```

`build-android.ps1` only builds the APK. It no longer edits any HTML timestamp or website file.

## Release APKs

APK binaries are not committed to this repository. Public APKs should be attached to GitHub Releases.

The release workflow expects these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

When a `v*` tag is pushed, GitHub Actions builds a signed release APK and uploads it as:

```text
haan-ghil-muulnaat.apk
```

## Archived Site

The old static project page is kept only as a repository-local archive under [docs/site](docs/site). This repository is not configured around GitHub Pages.

## License

MIT. See [LICENSE](LICENSE).

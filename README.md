# quizkama

A small Android app for drilling multiple-choice exam questions from a plain-text file.
Load a question dump, flip through the questions one at a time, pick an answer, and reveal
the correct one when you're ready.

## Features

- Loads questions from a `.txt` file picked with the system file picker (no storage permissions needed)
- Single-answer questions are shown as radio buttons, multi-answer questions as checkboxes
- Answer options are shuffled for every question; question order can be shuffled too (Settings)
- Reveal the correct answer: it's highlighted in blue
- Hands-free navigation with swipes and the volume keys
- Ignores `gratisexam` watermark lines that some exam dumps contain

## Preparing a question file from a gratisexam-PDF

Exam dumps from gratisexam come as PDFs, and the app only reads plain text, so convert them first:

1. Open the PDF in [Sumatra PDF](https://www.sumatrapdfreader.org/).
2. Choose **File > Save As...** and set the file type to **Text documents (\*.txt)**.
3. Copy the resulting `.txt` to the device.

## Usage

1. Put your question file anywhere on the device (or in a cloud storage the system picker can see).
2. Tap **Open** and choose the file in the system picker. The app remembers it: next time **Open**
   loads the same file right away. To pick another one use the menu: **Open...**.
3. Navigate:

| Action | Touch | Keys |
|---|---|---|
| Next question | Tap **Next** or swipe left | Volume Down |
| Previous question | Tap **Back** or swipe right | Volume Up |
| Reveal answer | Tap **Answer** or swipe up | — |

The **Settings** screen has the *Shuffle questions* switch.

## Building

Requirements: JDK 17 and the Android SDK (platform 35, build-tools 34.0.0).

- Android Gradle Plugin 8.7.3, Gradle 8.9 (wrapper included)
- `compileSdk` 35, `targetSdk` 34, `minSdk` 23
- AndroidX (AppCompat 1.7.0, ConstraintLayout 2.1.4)

```bash
./gradlew assembleDebug
```

Unit tests: `./gradlew testDebugUnitTest`.

Release signing isn't configured in the repo. Use your own keystore via
*Build > Generate Signed Bundle / APK* in Android Studio.

## CI/CD

GitHub Actions (`.github/workflows`):

- **CI** (`ci.yml`) runs on every push to `trunk` and on pull requests: unit tests
  (`testDebugUnitTest`) and a debug build; the debug APK and test reports are attached to the run.
- **Release** (`release.yml`) runs when a tag like `v2.12` is pushed
  (`git tag v2.12 && git push origin v2.12`). It builds the APK and publishes it as a GitHub Release.
  If the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`
  are set, the release APK is signed with your key; otherwise the debug build is published and the
  release is marked as a pre-release.

To create the keystore secret: `base64 -w0 release.jks` (on Windows: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))`).

## Project structure

```text
app/src/main/java/.../quizkama/
├── MainActivity.java         # UI, buttons, swipe and volume-key navigation
├── Parser.java               # Parses the QUESTION / A. / Correct Answer: text format
├── tectLoad.java             # Reads the chosen file via ContentResolver, runs the parser, shuffles
├── tectSharedFuncts.java     # Renders the current question, navigation, preferences
├── tectFragen.java           # Question model (text, options, correct answer)
├── OnSwipeTouchListener.java # Swipe gesture detection
└── SettingsActivity.java     # Preferences screen
```

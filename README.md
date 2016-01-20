# quizkama

A small Android app for drilling multiple-choice exam questions from a plain-text file.
Load a question dump, flip through the questions one at a time, pick an answer, and reveal
the correct one when you're ready.

## Features

- Loads questions from a `.txt` file on the device
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

1. Put your question file on the device. By default the app reads `Download/tect.txt`.
2. Tap **Open** to load it, or pick a different file via the menu: **Open...**.
3. Navigate:

| Action | Touch | Keys |
|---|---|---|
| Next question | Tap **Next** or swipe left | Volume Down |
| Previous question | Tap **Back** or swipe right | Volume Up |
| Reveal answer | Tap **Answer** or swipe up | — |

The **Settings** screen has the *Shuffle questions* switch.

## Building

This is a legacy project from 2017, and its toolchain is old:

- Android Gradle Plugin 3.0.1, Gradle 4.1 (wrapper included)
- `compileSdkVersion` / `targetSdkVersion` 26, `minSdkVersion` 23
- Android Support Library 26.1.0 (pre-AndroidX)

The build applies the `google-services` plugin, so it needs an `app/google-services.json`.
The file isn't committed. Download it from your own Firebase project for the package
`com.rasa.quizkama`, or remove the `com.google.gms.google-services` plugin and the
`firebase-auth` dependency from `app/build.gradle`. The app doesn't actually use Firebase.

```bash
./gradlew assembleDebug
```

> `jcenter()` is shut down, so a modern build will likely need `mavenCentral()` in its place
> and an upgraded AGP/Gradle.

Release signing isn't configured in the repo. Use your own keystore via
*Build > Generate Signed Bundle / APK* in Android Studio.

## Project structure

```text
app/src/main/java/.../quizkama/
├── MainActivity.java         # UI, buttons, swipe and volume-key navigation
├── Parser.java               # Parses the QUESTION / A. / Correct Answer: text format
├── tectLoad.java             # Reads the file, runs the parser, shuffles questions and options
├── tectSharedFuncts.java     # Renders the current question, navigation, preferences
├── tectFragen.java           # Question model (text, options, correct answer)
├── OpenFileDialog.java       # Simple file picker for .txt files
├── OnSwipeTouchListener.java # Swipe gesture detection
└── SettingsActivity.java     # Preferences screen
```

# PDF Pocket for Android

A lightweight native Android PDF opener/viewer.

## Features
- Pick a PDF using Android's system document picker.
- Accepts PDFs opened from compatible file managers and other apps.
- Page-by-page navigation.
- Pinch to zoom and drag while zoomed.
- No broad storage permission required. Uses Android's Storage Access Framework.
- Uses Android's built-in `PdfRenderer`.

## Build an APK from your phone using GitHub Actions

This project includes `.github/workflows/build-apk.yml`. GitHub Actions builds the APK in the cloud.

1. Download and extract this ZIP on your phone.
2. In a browser, sign in to GitHub and create a new repository. A free public repository is easiest for this workflow.
3. Upload the **contents** of the extracted `PDFPocketAndroid` folder to the repository, keeping folder paths intact. Make sure `.github/workflows/build-apk.yml` is included. GitHub's website may hide dot-folders in some upload flows; if so, use GitHub's web editor to create `.github/workflows/build-apk.yml` and paste in the workflow from this project.
4. Open the repository's **Actions** tab. Select **Build PDF Pocket APK** and tap **Run workflow** if it has not already started from the upload.
5. Wait for the run to finish successfully. Open that run and scroll to **Artifacts**.
6. Download `PDFPocket-debug-apk`, extract it, then tap `app-debug.apk` on your phone to install. If Android asks, allow your browser or file manager to install unknown apps.

The workflow uses Java 17, Android SDK 35 and Gradle 8.7. It creates a debug APK for testing. You do not need Android Studio on your phone.

## Build locally
Open this folder in Android Studio on Windows, macOS or Linux, sync Gradle, then select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Minimum Android version: Android 6.0 (API 23).

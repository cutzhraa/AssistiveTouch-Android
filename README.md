# 📱 AssistiveTouch Android (iOS Style)

A custom Android AssistiveTouch application built using Kotlin and Android Accessibility Services, designed to mirror the iOS AssistiveTouch user interface and system actions.

## 📥 Download
Get the ready-to-install APK directly from the [Latest Release](https://github.com/cutzhraa/AssistiveTouch-Android/releases/latest).

## ✨ Features
- **iOS Aesthetic & Snap-to-Edge**: Floating concentric circle button with smooth snap-to-edge behavior.
- **3x3 Grid Menu**: Quick access to Home, Back, Recent Apps, Notifications, Quick Settings, Screenshot, Lock Screen, and Voice Assistant.
- **Customizable Favorites**: Dedicated sub-menu allowing users to add, launch, replace (via long-press), or remove favorite installed applications.
- **Native Icons**: Automatically displays native application icons dynamically fetched from the system.
- **Accessibility Integration**: System-level controls powered by `AccessibilityService`.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **SDK**: Android SDK (API 24+)
- **Build Tool**: Gradle CLI
- **IDE / Environment**: VS Code

## 🚀 How to Build
To build the debug APK locally:

```cmd
.\gradle-8.5\bin\gradle.bat assembleDebug

The generated APK file will be located at:

app/build/outputs/apk/debug/app-debug.apk

Created by Cut Azizah Zahra
# 📱 AssistiveTouch Android (iOS Style)

A custom Android AssistiveTouch application built using Kotlin and Android Accessibility Services, designed to mirror the iOS AssistiveTouch user interface and system actions.

## ✨ Features
- **iOS Aesthetic & Snap-to-Edge**: Floating concentric circle button with smooth snap-to-edge behavior.
- **3x3 Grid Menu**: Quick access to Home, Back, Recent Apps, Notifications, Quick Settings, Screenshot, Lock Screen, and Gemini Voice Assistant.
- **Customizable Favorites**: Dedicated sub-menu allowing users to add, launch, replace, or remove favorite installed applications.
- **Accessibility Integration**: System-level controls powered by `AccessibilityService`.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **SDK**: Android SDK (API 24+)
- **Build Tool**: Gradle CLI
- **IDE / Environment**: VS Code

## 🚀 How to Build
```cmd
.\gradle-8.5\bin\gradle.bat assembleDebug

---

### Step 3: Push ke GitHub lewat Terminal VS Code

1. Buat repositori baru di GitHub (misalnya dengan nama **`AssistiveTouch-Android`**). Jangan centang *"Initialize this repository with a README"*.
2. Buka terminal di VS Code, lalu jalankan perintah berikut secara berurutan:

```cmd
git init
git add .
git commit -m "feat: initial commit for AssistiveTouch Android app"
git branch -M main
git remote add origin https://github.com/USERNAME/AssistiveTouch-Android.git
git push -u origin main
# 🌙 SyncRise — Sleep-Cycle & Mission-Based Smart Alarm App

![Android](https://img.shields.io/badge/Android-Java-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore%20%7C%20Storage%20%7C%20FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![CameraX](https://img.shields.io/badge/CameraX-ML%20Kit-00F0FF?style=for-the-badge)
![UI Theme](https://img.shields.io/badge/Design-Dark%20Glassmorphism-0F172A?style=for-the-badge)

**SyncRise** is an advanced Android application engineered to optimize human sleep architecture and guarantee wakefulness through gamified lock-screen missions and predictive sensor telemetry. Built with native Java, View Binding, and a modern Dark Glassmorphism aesthetic, SyncRise synchronizes wake-up times with natural REM sleep cycles to prevent grogginess.

---

## 👤 Student & Course Information

* **Developer:** Abu Fattah Md. Ismum
* **Roll:** 2307034
* **Department:** Computer Science and Engineering (CSE)
* **Institution:** Khulna University of Engineering & Technology (KUET)
* **Course:** Advanced Programming Laboratory

---

## ✨ Key Features & Walkthrough

### 🌅 1. Predictive "Smart Wake" Algorithm
- **30-Minute Light Sleep Window:** Instead of a jarring hard alarm, SyncRise monitors accelerometer movement vectors ($\Delta = |\text{magnitude} - g|$) in a 30-minute pre-alarm window.
- **Grogginess Prevention:** If movement spikes ($> 1.5\,\text{m/s}^2$) are detected—signifying a transition into **Light Sleep**—the alarm triggers early to wake you up feeling naturally refreshed.

### 🎯 2. Lock-Screen Wakeup Missions & Dynamic Snooze Rules
Disables the alarm only after the user completes a verified cognitive or physical mission over the lock screen:
- 🧮 **Math Problem Mission (Medium):** Requires solving 3 randomized arithmetic equations ($N_1 \times N_2 + N_3$). Allows 1 snooze (5 minutes).
- 📷 **Barcode Scanner Mission (Hard):** Uses **CameraX** and **Google ML Kit Vision** to scan a target barcode offline. **Snoozing is strictly disabled!**
- 💤 **No Mission (Easy):** Features a **Shrinking Snooze Penalty** ($5\text{m} \rightarrow 3\text{m} \rightarrow 1\text{m} \rightarrow \text{disabled}$) to force the user out of bed.

### 📝 3. Custom Pop-Up Reminders
- Allows users to attach custom pop-up notes during alarm creation (e.g., *"Drink water!"*, *"Exams today!"*, *"Bring calculator"*).
- The reminder renders prominently across the lock-screen mission and system notification banners.

### 📊 4. Sleep Debt Analytics & Hypnogram Line Charts
- **Sleep Debt Tracking:** Calculates a rolling 7-day Sleep Debt score against an ideal 56-hour threshold ($(7 \times 8\text{h}) - \text{Total Slept}$).
- **Overnight Hypnogram Graph:** Built with **MPAndroidChart** (`LineChart` in Stepped Mode) to map sleep logs into an overnight sleep architecture graph showing **Deep (0)**, **Light (1)**, **REM (2)**, and **Awake (3)** stages.
- **7-Day BarChart:** Visualizes daily sleep duration trends.

### 🎵 5. Sleep Soundscapes & Auto-Stop Timer
- Plays looping white noise, heavy rain, or ocean wave audio.
- Integrated with a **15-minute Auto-Stop CountDownTimer** that shuts off audio when the user falls asleep.

### 💡 6. Daily REST API Motivation Quotes
- Asynchronously fetches daily motivational advice from the public `adviceslip.com` REST API using native `HttpURLConnection` and `JSONObject` parsing.

---

## 🏗️ System Architecture & Android Components

```
                   ┌─────────────────────────────────────────┐
                   │             SyncRise App                │
                   └────────────────────┬────────────────────┘
                                        │
    ┌──────────────────┬────────────────┼──────────────────┬──────────────────┐
    ▼                  ▼                ▼                  ▼                  ▼
┌──────────────┐ ┌───────────┐  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  Activities  │ │  Services │  │  Receivers   │   │  Providers   │   │   Firebase   │
│  (9 Screens) │ │ (FGS Doze)│  │ (3 Custom)   │   │ (2 Custom)   │   │   Cloud      │
└──────────────┘ └───────────┘  └──────────────┘   └──────────────┘   └──────────────┘
```

* **Foreground Service (`SleepTrackingService`):** Runs with an active notification tray banner (`specialUse` FGS) to continuously monitor accelerometer vectors and Google Sleep API updates during Doze mode.
* **System Broadcast Receivers:**
  - `AlarmReceiver`: Manages exact `AlarmManager.setAlarmClock()` triggers and full-screen intents.
  - `SleepReceiver`: Handles background callbacks from the **Google Play Services Sleep API**.
  - `batteryReceiver`: Dynamically listens for `Intent.ACTION_BATTERY_LOW` to gracefully stop tracking if the battery drops critically low.
* **Content Providers:**
  - **Custom Provider (`SleepLogProvider`):** Wraps an `SQLiteOpenHelper` database (`syncrise.db`) for local offline sleep logs.
  - **System Provider (`MediaStore`):** Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` to allow custom alarm ringtone selection.

---

## ☁️ Firebase Cloud Ecosystem

1. **Firebase Authentication (`FirebaseAuth`):** Email/Password user login & registration (`LoginActivity.java`).
2. **Firebase Firestore (`FirebaseFirestore`):** Cloud NoSQL storage for alarms (`users/{uid}/alarms`) and sleep logs (`users/{uid}/sleepLogs`).
3. **Firebase Realtime Database (`FirebaseDatabase`):** Syncs live user timestamps to `users/{uid}/lastActive`.
4. **Firebase Cloud Storage (`FirebaseStorage`):** Stores user profile pictures (`profile_pictures/{uid}.jpg`) rendered via **Glide**.
5. **Firebase Cloud Messaging (`FirebaseMessagingService`):** Sends push notification alerts when Sleep Debt exceeds 5 hours.

---

## 🚀 Installation & Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/Hecker-Ismum/SyncRise.git
   ```
2. **Open in Android Studio:** Open the project in Android Studio Ladybug (2024.2.1) or newer.
3. **Add Firebase Config (`google-services.json`):**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `com.example.syncrise`.
   - Download `google-services.json` and place it inside the `app/` directory.
   - Enable **Authentication (Email/Password)**, **Cloud Firestore**, **Realtime Database**, and **Cloud Storage**.
4. **Build & Run:**
   - Connect an Android device or emulator (Android 8.0+ / API 26+).
   - Sync Gradle and run `app`.

---

## 📜 License & Copyright
Developed as an academic project for **KUET CSE Advanced Programming Laboratory**.
Copyright © 2025 Abu Fattah Md. Ismum. All rights reserved.

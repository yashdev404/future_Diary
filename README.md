# 📔 Future Diary

A premium, privacy-focused personal journaling application for Android. Future Diary combines the nostalgic feel of a physical notebook with modern cloud-sync technology and robust biometric security.

## 🌟 Key Features

### 🔐 Iron-Clad Privacy
*   **Biometric Lock**: Protect your memories with Fingerprint or Face ID.
*   **Encrypted Database**: Uses **SQLCipher** to encrypt your local database with hardware-backed keys (Android Keystore).
*   **Privacy First**: Cloud sync is entirely optional. Your data stays on your device unless you choose to back it up.

### ☁️ Seamless Cloud Sync
*   **Hybrid Storage**: Text entries are synced to **Firebase Firestore**, and photos to **Firebase Storage**.
*   **Anonymous Login**: Start writing immediately without a sign-up. Link an email later to secure your cloud backup.
*   **Multi-Device**: Access your journal on any device by linking your account.

### 🎵 Your Life's Soundtrack
*   **Music Integration**: Share songs directly from Spotify or YouTube into your diary.
*   **Metadata Fetching**: Automatically grabs album art, artists, and titles to create a "Soundtrack of your life."

### 📊 Emotional Insights
*   **Mood Tracking**: Track how you feel with every entry.
*   **Soul Portraits**: Monthly "Rewind" style cards that summarize your dominant moods and top written words.
*   **Soundtrack Summary**: See what music defined your month.

### 🛠️ Advanced Tools
*   **Memory Vault**: Lock specific entries away until a future date—a message to your future self.
*   **PDF Export**: Export your memories into beautiful, printable PDF documents.
*   **Daily Reminders**: Customizable notifications to help you maintain your writing streak.
*   **Rich Text**: Handwriting-style typography on custom "Vintage Parchment" digital paper.

## 🛠️ Tech Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM + Clean Architecture
*   **Local DB**: Room + SQLCipher (Encryption)
*   **DI**: Dagger-Hilt
*   **Backend**: Firebase (Auth, Firestore, Storage)
*   **Async**: Kotlin Coroutines + Flow
*   **Background Tasks**: WorkManager
*   **Utilities**: Coil (Images), Jsoup (Metadata), iText (PDF Export)

## 🎨 Design Philosophy
Future Diary follows a **Skeuomorphic** design approach. Every screen is designed to look and feel like a physical leather-bound book, featuring custom textures, parchment-colored "paper," and handwriting fonts.

## 📄 License
Copyright (C) 2026 Yash Shukla. Licensed under the Apache License, Version 2.0.

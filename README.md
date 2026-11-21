# SMS Reminder — Setup & Architecture Guide

## Overview
SMS Reminder combines a clean smartphone Notes experience with an advanced Alarm Clock and Daily Routine tracker.

This repository includes:
1. **Interactive Web Application (`/src/`)**: Full-featured React + TypeScript + Tailwind CSS application with persistent IndexedDB storage, real-time Web Audio API alarm tones, browser notifications, full-screen alarm screen, daily routine auto-reset, multi-select bulk deletion, and light/dark themes.
2. **Native Android Flutter + Kotlin Project (`/sms_reminder_flutter/`)**: Complete Flutter (Riverpod, GoRouter, Drift SQLite) and Kotlin (`AlarmManager`, `BroadcastReceiver`, `AlarmPlaybackService`, `BootReceiver`, `NotificationManagerHelper`, `MethodChannel`) source implementation ready to build in Android Studio.

## Android Studio / Flutter Build Steps
1. Open `/sms_reminder_flutter` in Android Studio or a terminal with Flutter SDK 3.22+ and Android SDK 34 installed.
2. Run Drift code generation and fetch packages:
   ```bash
   flutter pub get
   dart run build_runner build --delete-conflicting-outputs
   ```
3. Build a Debug APK:
   ```bash
   flutter build apk --debug
   ```
4. Build a Google Play-ready App Bundle (AAB) with release signing configured in `android/key.properties`:
   ```bash
   flutter build appbundle --release
   ```

## Database Tables (Drift SQLite)
- `reminders` (indexed on `scheduled_datetime`, `status`, `category_id`)
- `routine_templates` (indexed on `sort_order`)
- `daily_routine_completions` (indexed on `date_key`, foreign key cascade on `routine_templates`)
- `categories`
- `user_settings_table`

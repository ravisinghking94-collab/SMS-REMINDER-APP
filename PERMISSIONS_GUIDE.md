# SMS Reminder — Android Permissions Configuration Guide

## Declared Permissions (`AndroidManifest.xml`)

| Permission | Android API | Purpose | Fallback if Denied |
| :--- | :--- | :--- | :--- |
| `POST_NOTIFICATIONS` | Android 13+ (API 33+) | Displays high-priority reminder title & message notifications | Show in-app prompt explaining why notifications are needed; link to app notification settings |
| `SCHEDULE_EXACT_ALARM` | Android 12+ (API 31+) | Schedules wake-up alarms via `AlarmManager.setAlarmClock` | Detect via `AlarmManager.canScheduleExactAlarms()`; never claim exact alarm is scheduled if denied; open `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` |
| `USE_FULL_SCREEN_INTENT` | Android 14+ (API 34+) | Displays full-screen alarm UI when device is locked | Check `NotificationManager.canUseFullScreenIntent()`; offer `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` settings link |
| `RECEIVE_BOOT_COMPLETED` | All | Runs `BootReceiver` on reboot, `TIME_SET`, and `TIMEZONE_CHANGED` to restore active alarms | N/A (normal permission) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14+ | Keeps `AlarmPlaybackService` running reliably with `AudioAttributes.USAGE_ALARM` | Standard notification sound fallback |
| `VIBRATE` & `WAKE_LOCK` | All | Vibrates and wakes CPU during alarm trigger | Respects user device vibration / Do Not Disturb settings |

## Reliability Limitations
- Android devices cannot trigger alarms when **powered completely off**.
- If the user **force-stops** the app from Android App Info, Android halts scheduled alarms until the app is opened again.
- Aggressive OEM battery savers may delay background work unless battery optimization exemption is granted by the user.

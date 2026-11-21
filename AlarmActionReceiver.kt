package com.smsreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "com.smsreminder.app.ACTION_NOTIFICATION_STOP"
        const val ACTION_SNOOZE = "com.smsreminder.app.ACTION_NOTIFICATION_SNOOZE"
        const val ACTION_COMPLETE = "com.smsreminder.app.ACTION_NOTIFICATION_COMPLETE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(AlarmPlaybackService.EXTRA_REMINDER_ID) ?: ""
        val title = intent.getStringExtra(AlarmPlaybackService.EXTRA_TITLE) ?: "Reminder"
        val message = intent.getStringExtra(AlarmPlaybackService.EXTRA_MESSAGE) ?: ""
        val sound = intent.getStringExtra(AlarmPlaybackService.EXTRA_SOUND) ?: "royal_chime"
        val vibration = intent.getBooleanExtra(AlarmPlaybackService.EXTRA_VIBRATION, true)
        val snoozeMins = intent.getIntExtra(AlarmPlaybackService.EXTRA_SNOOZE_MINS, 10)
        val gradual = intent.getBooleanExtra(AlarmPlaybackService.EXTRA_GRADUAL_VOLUME, true)

        val stopService = Intent(context, AlarmPlaybackService::class.java).apply {
            action = AlarmPlaybackService.ACTION_STOP
        }
        context.startService(stopService)

        when (intent.action) {
            ACTION_SNOOZE -> {
                val triggerAt = System.currentTimeMillis() + snoozeMins * 60_000L
                AlarmScheduler.scheduleExactAlarm(
                    context = context,
                    reminderId = reminderId,
                    title = title,
                    message = message,
                    triggerAtMillis = triggerAt,
                    alarmSound = sound,
                    vibrationEnabled = vibration,
                    notificationEnabled = true,
                    snoozeDurationMinutes = snoozeMins,
                    gradualVolume = gradual
                )
            }
            ACTION_COMPLETE, ACTION_STOP -> {
                NotificationManagerHelper.cancelAlarmNotification(context, reminderId)
            }
        }
    }
}

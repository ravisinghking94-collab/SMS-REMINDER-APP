package com.smsreminder.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.smsreminder.app.MainActivity

object AlarmScheduler {

    fun requestCodeForReminder(reminderId: String): Int {
        // Stable non-zero positive requestCode derived from reminder UUID
        return reminderId.hashCode() and 0x7FFFFFFF
    }

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleExactAlarm(
        context: Context,
        reminderId: String,
        title: String,
        message: String,
        triggerAtMillis: Long,
        alarmSound: String,
        vibrationEnabled: Boolean,
        notificationEnabled: Boolean,
        snoozeDurationMinutes: Int,
        gradualVolume: Boolean
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!canScheduleExact(context)) {
            return false
        }

        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmPlaybackService.EXTRA_TITLE, title)
            putExtra(AlarmPlaybackService.EXTRA_MESSAGE, message)
            putExtra(AlarmPlaybackService.EXTRA_SOUND, alarmSound)
            putExtra(AlarmPlaybackService.EXTRA_VIBRATION, vibrationEnabled)
            putExtra(AlarmPlaybackService.EXTRA_NOTIFICATION, notificationEnabled)
            putExtra(AlarmPlaybackService.EXTRA_SNOOZE_MINS, snoozeDurationMinutes)
            putExtra(AlarmPlaybackService.EXTRA_GRADUAL_VOLUME, gradualVolume)
        }

        val requestCode = requestCodeForReminder(reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        return true
    }

    fun cancelAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val requestCode = requestCodeForReminder(reminderId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

package com.smsreminder.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smsreminder.app.MainActivity
import com.smsreminder.app.R

object NotificationManagerHelper {
    const val CHANNEL_ID_ALARM = "sms_reminder_alarms_channel"
    const val CHANNEL_ID_REMINDER = "sms_reminder_notifications_channel"
    const val NOTIFICATION_ID_ALARM = 9001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmChannel = NotificationChannel(
            CHANNEL_ID_ALARM,
            "Reminder Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority full-screen alarm alerts for SMS Reminder"
            setBypassDnd(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null) // Audio handled by AlarmPlaybackService USAGE_ALARM
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "Scheduled Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Standard reminder notifications"
        }

        nm.createNotificationChannel(alarmChannel)
        nm.createNotificationChannel(reminderChannel)
    }

    fun buildFullScreenAlarmNotification(
        context: Context,
        reminderId: String,
        title: String,
        message: String,
        sound: String,
        vibrationEnabled: Boolean,
        snoozeMins: Int,
        gradualVolume: Boolean
    ): Notification {
        ensureChannels(context)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmPlaybackService.EXTRA_TITLE, title)
            putExtra(AlarmPlaybackService.EXTRA_MESSAGE, message)
            putExtra(AlarmPlaybackService.EXTRA_SOUND, sound)
            putExtra(AlarmPlaybackService.EXTRA_SNOOZE_MINS, snoozeMins)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCodeForReminder(reminderId),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP
            putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId)
        }
        val stopPending = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.requestCodeForReminder(reminderId) + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmPlaybackService.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmPlaybackService.EXTRA_TITLE, title)
            putExtra(AlarmPlaybackService.EXTRA_MESSAGE, message)
            putExtra(AlarmPlaybackService.EXTRA_SOUND, sound)
            putExtra(AlarmPlaybackService.EXTRA_VIBRATION, vibrationEnabled)
            putExtra(AlarmPlaybackService.EXTRA_SNOOZE_MINS, snoozeMins)
            putExtra(AlarmPlaybackService.EXTRA_GRADUAL_VOLUME, gradualVolume)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.requestCodeForReminder(reminderId) + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(if (message.isNotEmpty()) message else "Scheduled reminder alarm")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Stop", stopPending)
            .addAction(0, "Snooze ${snoozeMins}m", snoozePending)
            .build()
    }

    fun cancelAlarmNotification(context: Context, reminderId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID_ALARM)
        if (reminderId.isNotEmpty()) {
            nm.cancel(AlarmScheduler.requestCodeForReminder(reminderId))
        }
    }
}

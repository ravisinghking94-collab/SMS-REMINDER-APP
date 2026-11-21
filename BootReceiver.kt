package com.smsreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import java.io.File

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        restoreActiveAlarms(context)
    }

    private fun restoreActiveAlarms(context: Context) {
        val dbFile = File(context.filesDir.parentFile, "app_flutter/sms_reminder.sqlite")
        if (!dbFile.exists()) return

        val now = System.currentTimeMillis()
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        db.use { sqlite ->
            val cursor = sqlite.rawQuery(
                """
                SELECT id, title, message, scheduled_datetime, snoozed_until, alarm_sound,
                       vibration_enabled, notification_enabled, snooze_duration, status
                FROM reminders
                WHERE alarm_enabled = 1 AND status IN ('active', 'snoozed')
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val title = c.getString(1)
                    val message = c.getString(2) ?: ""
                    val scheduledMillis = c.getLong(3)
                    val snoozedMillis = if (c.isNull(4)) null else c.getLong(4)
                    val alarmSound = c.getString(5) ?: "royal_chime"
                    val vibration = c.getInt(6) == 1
                    val notification = c.getInt(7) == 1
                    val snoozeMins = c.getInt(8)
                    val status = c.getString(9)

                    val triggerAt = if (status == "snoozed" && snoozedMillis != null) {
                        snoozedMillis
                    } else {
                        scheduledMillis
                    }

                    if (triggerAt > now) {
                        AlarmScheduler.cancelAlarm(context, id)
                        AlarmScheduler.scheduleExactAlarm(
                            context = context,
                            reminderId = id,
                            title = title,
                            message = message,
                            triggerAtMillis = triggerAt,
                            alarmSound = alarmSound,
                            vibrationEnabled = vibration,
                            notificationEnabled = notification,
                            snoozeDurationMinutes = snoozeMins,
                            gradualVolume = true
                        )
                    }
                }
            }
        }
    }
}

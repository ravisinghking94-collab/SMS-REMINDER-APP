package com.smsreminder.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ALARM_TRIGGER = "com.smsreminder.app.ACTION_ALARM_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmPlaybackService::class.java).apply {
            action = AlarmPlaybackService.ACTION_START
            putExtras(intent)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

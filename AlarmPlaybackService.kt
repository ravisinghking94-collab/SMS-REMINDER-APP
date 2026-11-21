package com.smsreminder.app.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmPlaybackService : Service() {
    companion object {
        const val ACTION_START = "com.smsreminder.app.SERVICE_START_ALARM"
        const val ACTION_STOP = "com.smsreminder.app.SERVICE_STOP_ALARM"

        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SOUND = "alarmSound"
        const val EXTRA_VIBRATION = "vibrationEnabled"
        const val EXTRA_NOTIFICATION = "notificationEnabled"
        const val EXTRA_SNOOZE_MINS = "snoozeDurationMinutes"
        const val EXTRA_GRADUAL_VOLUME = "gradualVolume"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentVolume = 0.15f
    private var activeReminderId: String = ""

    private val volumeRampRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return
            if (currentVolume < 1.0f) {
                currentVolume = (currentVolume + 0.08f).coerceAtMost(1.0f)
                mp.setVolume(currentVolume, currentVolume)
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarmPlayback()
            stopSelf()
            return START_NOT_STICKY
        }

        val reminderId = intent?.getStringExtra(EXTRA_REMINDER_ID) ?: "alarm"
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "SMS Reminder"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: ""
        val sound = intent?.getStringExtra(EXTRA_SOUND) ?: "royal_chime"
        val vibrationEnabled = intent?.getBooleanExtra(EXTRA_VIBRATION, true) ?: true
        val snoozeMins = intent?.getIntExtra(EXTRA_SNOOZE_MINS, 10) ?: 10
        val gradualVolume = intent?.getBooleanExtra(EXTRA_GRADUAL_VOLUME, true) ?: true

        activeReminderId = reminderId

        val notification = NotificationManagerHelper.buildFullScreenAlarmNotification(
            context = this,
            reminderId = reminderId,
            title = title,
            message = message,
            sound = sound,
            vibrationEnabled = vibrationEnabled,
            snoozeMins = snoozeMins,
            gradualVolume = gradualVolume
        )

        startForeground(NotificationManagerHelper.NOTIFICATION_ID_ALARM, notification)
        startAlarmAudio(gradualVolume)
        if (vibrationEnabled) startVibration()

        return START_STICKY
    }

    private fun startAlarmAudio(gradual: Boolean) {
        stopAlarmAudio()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(applicationContext, alarmUri)
            isLooping = true
            prepare()
            currentVolume = if (gradual) 0.15f else 1.0f
            setVolume(currentVolume, currentVolume)
            start()
        }

        if (gradual) {
            handler.postDelayed(volumeRampRunnable, 1000L)
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 300, 500, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmAudio() {
        handler.removeCallbacks(volumeRampRunnable)
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private fun stopAlarmPlayback() {
        stopAlarmAudio()
        vibrator?.cancel()
        vibrator = null
        NotificationManagerHelper.cancelAlarmNotification(this, activeReminderId)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopAlarmPlayback()
        super.onDestroy()
    }
}

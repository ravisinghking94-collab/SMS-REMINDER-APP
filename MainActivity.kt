package com.smsreminder.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smsreminder.app.alarm.AlarmPlaybackService
import com.smsreminder.app.alarm.AlarmScheduler
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        const val CHANNEL_NAME = "com.smsreminder.app/alarm_engine"
        private const val REQ_POST_NOTIFICATIONS = 1001
    }

    private var pendingPermissionResult: MethodChannel.Result? = null
    private var methodChannel: MethodChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        handleAlarmLaunchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmLaunchIntent(intent)
    }

    private fun handleAlarmLaunchIntent(intent: Intent?) {
        val reminderId = intent?.getStringExtra(AlarmPlaybackService.EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(AlarmPlaybackService.EXTRA_TITLE) ?: "Reminder"
        val message = intent.getStringExtra(AlarmPlaybackService.EXTRA_MESSAGE) ?: ""
        val sound = intent.getStringExtra(AlarmPlaybackService.EXTRA_SOUND) ?: "royal_chime"
        val snoozeMins = intent.getIntExtra(AlarmPlaybackService.EXTRA_SNOOZE_MINS, 10)

        methodChannel?.invokeMethod(
            "onAlarmTriggered",
            mapOf(
                "reminderId" to reminderId,
                "title" to title,
                "message" to message,
                "alarmSound" to sound,
                "snoozeDurationMinutes" to snoozeMins
            )
        )
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
        methodChannel = channel

        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "scheduleAlarm" -> {
                    val reminderId = call.argument<String>("reminderId")
                    val title = call.argument<String>("title") ?: "Reminder"
                    val message = call.argument<String>("message") ?: ""
                    val triggerAtMillis = call.argument<Number>("triggerAtMillis")?.toLong()
                    val alarmSound = call.argument<String>("alarmSound") ?: "royal_chime"
                    val vibrationEnabled = call.argument<Boolean>("vibrationEnabled") ?: true
                    val notificationEnabled = call.argument<Boolean>("notificationEnabled") ?: true
                    val snoozeDurationMinutes = call.argument<Int>("snoozeDurationMinutes") ?: 10
                    val gradualVolume = call.argument<Boolean>("gradualVolume") ?: true

                    if (reminderId == null || triggerAtMillis == null) {
                        result.error("INVALID_ARGS", "Missing reminderId or triggerAtMillis", null)
                        return@setMethodCallHandler
                    }

                    val scheduled = AlarmScheduler.scheduleExactAlarm(
                        context = applicationContext,
                        reminderId = reminderId,
                        title = title,
                        message = message,
                        triggerAtMillis = triggerAtMillis,
                        alarmSound = alarmSound,
                        vibrationEnabled = vibrationEnabled,
                        notificationEnabled = notificationEnabled,
                        snoozeDurationMinutes = snoozeDurationMinutes,
                        gradualVolume = gradualVolume
                    )
                    result.success(scheduled)
                }

                "cancelAlarm" -> {
                    val reminderId = call.argument<String>("reminderId")
                    if (reminderId != null) {
                        AlarmScheduler.cancelAlarm(applicationContext, reminderId)
                    }
                    result.success(null)
                }

                "cancelMultipleAlarms" -> {
                    val ids = call.argument<List<String>>("reminderIds") ?: emptyList()
                    ids.forEach { AlarmScheduler.cancelAlarm(applicationContext, it) }
                    result.success(null)
                }

                "stopActiveAlarm" -> {
                    val stopIntent = Intent(applicationContext, AlarmPlaybackService::class.java).apply {
                        action = AlarmPlaybackService.ACTION_STOP
                    }
                    applicationContext.startService(stopIntent)
                    result.success(null)
                }

                "snoozeActiveAlarm" -> {
                    val reminderId = call.argument<String>("reminderId") ?: ""
                    val title = call.argument<String>("title") ?: "Reminder"
                    val message = call.argument<String>("message") ?: ""
                    val snoozeMinutes = call.argument<Int>("snoozeMinutes") ?: 10
                    val alarmSound = call.argument<String>("alarmSound") ?: "royal_chime"
                    val vibrationEnabled = call.argument<Boolean>("vibrationEnabled") ?: true
                    val gradualVolume = call.argument<Boolean>("gradualVolume") ?: true

                    val stopIntent = Intent(applicationContext, AlarmPlaybackService::class.java).apply {
                        action = AlarmPlaybackService.ACTION_STOP
                    }
                    applicationContext.startService(stopIntent)

                    val triggerAt = System.currentTimeMillis() + snoozeMinutes * 60_000L
                    AlarmScheduler.scheduleExactAlarm(
                        context = applicationContext,
                        reminderId = reminderId,
                        title = title,
                        message = message,
                        triggerAtMillis = triggerAt,
                        alarmSound = alarmSound,
                        vibrationEnabled = vibrationEnabled,
                        notificationEnabled = true,
                        snoozeDurationMinutes = snoozeMinutes,
                        gradualVolume = gradualVolume
                    )
                    result.success(null)
                }

                "checkPermissions" -> {
                    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }

                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val exactAlarmsPermitted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        alarmManager.canScheduleExactAlarms()
                    } else {
                        true
                    }

                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val fullScreenPermitted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        nm.canUseFullScreenIntent()
                    } else {
                        true
                    }

                    result.success(
                        mapOf(
                            "notificationsGranted" to notificationsGranted,
                            "exactAlarmsPermitted" to exactAlarmsPermitted,
                            "fullScreenIntentPermitted" to fullScreenPermitted
                        )
                    )
                }

                "requestNotificationPermission" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            result.success(true)
                        } else {
                            pendingPermissionResult = result
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                REQ_POST_NOTIFICATIONS
                            )
                        }
                    } else {
                        result.success(true)
                    }
                }

                "openExactAlarmSettings" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(i)
                    }
                    result.success(null)
                }

                "openFullScreenIntentSettings" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val i = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(i)
                    }
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            pendingPermissionResult?.success(granted)
            pendingPermissionResult = null
        }
    }
}

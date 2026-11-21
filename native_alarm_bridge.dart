import 'package:flutter/services.dart';

class PermissionStatusSnapshot {
  final bool notificationsGranted;
  final bool exactAlarmsPermitted;
  final bool fullScreenIntentPermitted;

  const PermissionStatusSnapshot({
    required this.notificationsGranted,
    required this.exactAlarmsPermitted,
    required this.fullScreenIntentPermitted,
  });
}

class NativeAlarmBridge {
  static const MethodChannel _channel =
      MethodChannel('com.smsreminder.app/alarm_engine');

  Future<bool> scheduleReminderAlarm({
    required String reminderId,
    required String title,
    required String message,
    required int triggerAtMillis,
    required String alarmSound,
    required bool vibrationEnabled,
    required bool notificationEnabled,
    required int snoozeDurationMinutes,
    required bool gradualVolume,
  }) async {
    final result = await _channel.invokeMethod<bool>('scheduleAlarm', {
      'reminderId': reminderId,
      'title': title,
      'message': message,
      'triggerAtMillis': triggerAtMillis,
      'alarmSound': alarmSound,
      'vibrationEnabled': vibrationEnabled,
      'notificationEnabled': notificationEnabled,
      'snoozeDurationMinutes': snoozeDurationMinutes,
      'gradualVolume': gradualVolume,
    });
    return result ?? false;
  }

  Future<void> cancelReminderAlarm(String reminderId) async {
    await _channel.invokeMethod<void>('cancelAlarm', {
      'reminderId': reminderId,
    });
  }

  Future<void> cancelMultipleAlarms(List<String> reminderIds) async {
    await _channel.invokeMethod<void>('cancelMultipleAlarms', {
      'reminderIds': reminderIds,
    });
  }

  Future<void> stopActiveAlarm(String reminderId) async {
    await _channel.invokeMethod<void>('stopActiveAlarm', {
      'reminderId': reminderId,
    });
  }

  Future<void> snoozeActiveAlarm({
    required String reminderId,
    required String title,
    required String message,
    required int snoozeMinutes,
    required String alarmSound,
    required bool vibrationEnabled,
    required bool gradualVolume,
  }) async {
    await _channel.invokeMethod<void>('snoozeActiveAlarm', {
      'reminderId': reminderId,
      'title': title,
      'message': message,
      'snoozeMinutes': snoozeMinutes,
      'alarmSound': alarmSound,
      'vibrationEnabled': vibrationEnabled,
      'gradualVolume': gradualVolume,
    });
  }

  Future<PermissionStatusSnapshot> checkPermissions() async {
    final map = await _channel.invokeMapMethod<String, bool>('checkPermissions');
    return PermissionStatusSnapshot(
      notificationsGranted: map?['notificationsGranted'] ?? false,
      exactAlarmsPermitted: map?['exactAlarmsPermitted'] ?? false,
      fullScreenIntentPermitted: map?['fullScreenIntentPermitted'] ?? false,
    );
  }

  Future<bool> requestNotificationPermission() async {
    return (await _channel.invokeMethod<bool>('requestNotificationPermission')) ??
        false;
  }

  Future<void> openExactAlarmSettings() async {
    await _channel.invokeMethod<void>('openExactAlarmSettings');
  }

  Future<void> openFullScreenIntentSettings() async {
    await _channel.invokeMethod<void>('openFullScreenIntentSettings');
  }
}

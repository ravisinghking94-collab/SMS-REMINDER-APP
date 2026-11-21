import 'package:drift/drift.dart';
import '../database/app_database.dart';
import '../../services/native_alarm_bridge.dart';

class ReminderRepository {
  final AppDatabase _db;
  final NativeAlarmBridge _alarmBridge;

  ReminderRepository(this._db, this._alarmBridge);

  Stream<List<Reminder>> watchAllReminders() {
    return (_db.select(_db.reminders)
          ..orderBy([(t) => OrderingTerm.asc(t.scheduledDatetime)]))
        .watch();
  }

  Future<bool> saveAndScheduleReminder({
    required RemindersCompanion companion,
    required String id,
    required String title,
    required String message,
    required int triggerAtMillis,
    required bool alarmEnabled,
    required bool notificationEnabled,
    required String alarmSound,
    required bool vibrationEnabled,
    required int snoozeDurationMinutes,
    required bool gradualVolume,
  }) async {
    await _db.into(_db.reminders).insertOnConflictUpdate(companion);

    // Cancel any existing alarm before rescheduling to guarantee zero duplicates
    await _alarmBridge.cancelReminderAlarm(id);

    if (alarmEnabled) {
      return _alarmBridge.scheduleReminderAlarm(
        reminderId: id,
        title: title,
        message: message,
        triggerAtMillis: triggerAtMillis,
        alarmSound: alarmSound,
        vibrationEnabled: vibrationEnabled,
        notificationEnabled: notificationEnabled,
        snoozeDurationMinutes: snoozeDurationMinutes,
        gradualVolume: gradualVolume,
      );
    }
    return true;
  }

  Future<void> deleteSingleReminder(String id) async {
    await _alarmBridge.cancelReminderAlarm(id);
    await (_db.delete(_db.reminders)..where((t) => t.id.equals(id))).go();
  }

  Future<void> deleteMultipleReminders(List<String> ids) async {
    if (ids.isEmpty) return;
    await _alarmBridge.cancelMultipleAlarms(ids);
    await (_db.delete(_db.reminders)..where((t) => t.id.isIn(ids))).go();
  }
}

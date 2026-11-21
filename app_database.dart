import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

part 'app_database.g.dart';

@TableIndex(name: 'idx_reminders_scheduled', columns: {#scheduledDatetime})
@TableIndex(name: 'idx_reminders_status', columns: {#status})
@TableIndex(name: 'idx_reminders_category', columns: {#categoryId})
class Reminders extends Table {
  TextColumn get id => text()();
  TextColumn get title => text().withLength(min: 1, max: 200)();
  TextColumn get message => text().withDefault(const Constant(''))();
  IntColumn get scheduledDatetime => integer()(); // UTC epoch millis
  TextColumn get timezoneId => text()();
  TextColumn get categoryId =>
      text().references(Categories, #id, onDelete: KeyAction.setDefault).withDefault(const Constant('cat_other'))();
  TextColumn get priority => text().withDefault(const Constant('medium'))();
  TextColumn get repeatType => text().withDefault(const Constant('never'))();
  TextColumn get repeatDays => text().withDefault(const Constant('[]'))();
  BoolColumn get alarmEnabled => boolean().withDefault(const Constant(true))();
  BoolColumn get notificationEnabled =>
      boolean().withDefault(const Constant(true))();
  TextColumn get alarmSound =>
      text().withDefault(const Constant('royal_chime'))();
  BoolColumn get vibrationEnabled =>
      boolean().withDefault(const Constant(true))();
  IntColumn get snoozeDuration => integer().withDefault(const Constant(10))();
  TextColumn get status => text().withDefault(const Constant('active'))();
  IntColumn get snoozedUntil => integer().nullable()();
  IntColumn get completedAt => integer().nullable()();
  IntColumn get createdAt => integer()();
  IntColumn get updatedAt => integer()();

  @override
  Set<Column> get primaryKey => {id};
}

@TableIndex(name: 'idx_routine_sort', columns: {#sortOrder})
class RoutineTemplates extends Table {
  TextColumn get id => text()();
  TextColumn get title => text().withLength(min: 1, max: 160)();
  TextColumn get description => text().withDefault(const Constant(''))();
  TextColumn get scheduledTime => text()(); // "HH:mm"
  TextColumn get period => text().withDefault(const Constant('morning'))();
  TextColumn get categoryId =>
      text().references(Categories, #id, onDelete: KeyAction.setDefault).withDefault(const Constant('cat_other'))();
  TextColumn get priority => text().withDefault(const Constant('medium'))();
  TextColumn get repeatType => text().withDefault(const Constant('daily'))();
  TextColumn get repeatDays => text().withDefault(const Constant('[]'))();
  BoolColumn get alarmEnabled => boolean().withDefault(const Constant(false))();
  TextColumn get alarmSound =>
      text().withDefault(const Constant('royal_chime'))();
  IntColumn get sortOrder => integer().withDefault(const Constant(0))();
  IntColumn get createdAt => integer()();
  IntColumn get updatedAt => integer()();

  @override
  Set<Column> get primaryKey => {id};
}

@TableIndex(name: 'idx_routine_completion_date', columns: {#dateKey})
class DailyRoutineCompletions extends Table {
  TextColumn get id => text()();
  TextColumn get templateId =>
      text().references(RoutineTemplates, #id, onDelete: KeyAction.cascade)();
  TextColumn get dateKey => text()(); // "YYYY-MM-DD"
  BoolColumn get completed => boolean().withDefault(const Constant(false))();
  IntColumn get completedAt => integer().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

class Categories extends Table {
  TextColumn get id => text()();
  TextColumn get name => text().withLength(min: 1, max: 60)();
  TextColumn get colorHex => text()();
  TextColumn get iconName => text()();
  BoolColumn get isDefault => boolean().withDefault(const Constant(false))();

  @override
  Set<Column> get primaryKey => {id};
}

class UserSettingsTable extends Table {
  TextColumn get key => text()();
  TextColumn get value => text()();

  @override
  Set<Column> get primaryKey => {key};
}

@DriftDatabase(
  tables: [
    Reminders,
    RoutineTemplates,
    DailyRoutineCompletions,
    Categories,
    UserSettingsTable,
  ],
)
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 2;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (Migrator m) async {
          await m.createAll();
          await _seedDefaultCategories();
        },
        onUpgrade: (Migrator m, int from, int to) async {
          if (from < 2) {
            await m.addColumn(reminders, reminders.snoozedUntil);
          }
        },
        beforeOpen: (details) async {
          await customStatement('PRAGMA foreign_keys = ON');
        },
      );

  Future<void> _seedDefaultCategories() async {
    final defaults = [
      CategoriesCompanion.insert(
          id: 'cat_personal',
          name: 'Personal',
          colorHex: '#2557D6',
          iconName: 'person',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_work',
          name: 'Work',
          colorHex: '#4F46E5',
          iconName: 'work',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_health',
          name: 'Health',
          colorHex: '#16A34A',
          iconName: 'favorite',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_shopping',
          name: 'Shopping',
          colorHex: '#D97706',
          iconName: 'shopping_bag',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_family',
          name: 'Family',
          colorHex: '#DB2777',
          iconName: 'home',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_study',
          name: 'Study',
          colorHex: '#0284C7',
          iconName: 'menu_book',
          isDefault: const Value(true)),
      CategoriesCompanion.insert(
          id: 'cat_other',
          name: 'Other',
          colorHex: '#64748B',
          iconName: 'label',
          isDefault: const Value(true)),
    ];
    for (final cat in defaults) {
      await into(categories).insertOnConflictUpdate(cat);
    }
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'sms_reminder.sqlite'));
    return NativeDatabase.createInBackground(file);
  });
}

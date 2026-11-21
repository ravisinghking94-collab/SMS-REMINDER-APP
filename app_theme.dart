import 'package:flutter/material.dart';

class AppTheme {
  static const Color primaryRoyalBlue = Color(0xFF2557D6);
  static const Color secondaryLavender = Color(0xFFEEF2FF);
  static const Color deepNavy = Color(0xFF0F172A);
  static const Color mutedGrey = Color(0xFF64748B);
  static const Color surfaceLight = Color(0xFFF8FAFC);

  static ThemeData get lightTheme {
    final scheme = ColorScheme.fromSeed(
      seedColor: primaryRoyalBlue,
      brightness: Brightness.light,
      primary: primaryRoyalBlue,
      secondary: secondaryLavender,
      surface: Colors.white,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: surfaceLight,
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.white,
        foregroundColor: deepNavy,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Color(0xFFE2E8F0)),
        ),
      ),
    );
  }

  static ThemeData get darkTheme {
    final scheme = ColorScheme.fromSeed(
      seedColor: primaryRoyalBlue,
      brightness: Brightness.dark,
      primary: primaryRoyalBlue,
      surface: const Color(0xFF0F172A),
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: const Color(0xFF0B1120),
      cardTheme: CardThemeData(
        color: const Color(0xFF0F172A),
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Color(0xFF1E293B)),
        ),
      ),
    );
  }
}

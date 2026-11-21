import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/home',
    routes: [
      GoRoute(
        path: '/home',
        builder: (context, state) => const _RouteStub(title: 'Home'),
      ),
      GoRoute(
        path: '/reminders',
        builder: (context, state) => const _RouteStub(title: 'My Reminders'),
      ),
      GoRoute(
        path: '/routine',
        builder: (context, state) => const _RouteStub(title: 'Daily Routine'),
      ),
      GoRoute(
        path: '/completed',
        builder: (context, state) => const _RouteStub(title: 'Completed'),
      ),
      GoRoute(
        path: '/settings',
        builder: (context, state) => const _RouteStub(title: 'Settings'),
      ),
    ],
  );
});

class _RouteStub extends StatelessWidget {
  final String title;
  const _RouteStub({required this.title});

  @override
  Widget build(BuildContext context) {
    return Scaffold(appBar: AppBar(title: Text(title)));
  }
}

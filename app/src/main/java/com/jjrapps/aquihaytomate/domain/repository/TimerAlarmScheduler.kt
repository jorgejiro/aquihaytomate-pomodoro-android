package com.jjrapps.aquihaytomate.domain.repository

/**
 * Layer 3 of the engine: the safety net. An `AlarmManager` alarm at the same deadline as the service,
 * so a slot still fires if the OEM killed the foreground service or the process died.
 *
 * Exact when the user granted `SCHEDULE_EXACT_ALARM`, inexact otherwise — **the app has to work
 * without it**, which is why the alarm is the backstop and not the primary mechanism. See
 * docs/decisions/002-motor-del-temporizador-hibrido.md.
 */
interface TimerAlarmScheduler {

    /**
     * Arms the backup alarm.
     *
     * @param deadlineElapsedRealtimeMs the monotonic deadline, matching
     *   `TimerState.endAtElapsedRealtimeMs`. `ELAPSED_REALTIME_WAKEUP` is used rather than an RTC
     *   alarm so moving the system clock cannot retime it.
     * @return true when the alarm was scheduled exactly.
     */
    fun arm(deadlineElapsedRealtimeMs: Long): Boolean

    fun cancel()

    /** Whether exact alarms are available, for the warning in Settings. */
    fun canScheduleExactAlarms(): Boolean
}

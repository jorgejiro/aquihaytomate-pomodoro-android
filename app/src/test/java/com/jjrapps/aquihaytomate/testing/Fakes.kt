package com.jjrapps.aquihaytomate.testing

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.FocusSession
import com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.repository.ChargingMonitor
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerAlarmScheduler
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerServiceController
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory stand-ins for the repositories, so the command use cases can be driven end to end on the
 * JVM. They mimic the two properties the real implementations are built on: `update` is atomic, and
 * `record` is idempotent on `(sessionId, slotIndex)`.
 */
class FakeTimerStateRepository(initial: TimerState = TimerState.EMPTY) : TimerStateRepository {

    private val flow = MutableStateFlow(initial)
    private val mutex = Mutex()

    /** How many writes actually landed, to catch a transition being applied twice. */
    var writeCount: Int = 0
        private set

    override val state: Flow<TimerState> = flow.asStateFlow()

    override suspend fun current(): TimerState = flow.value

    override suspend fun write(state: TimerState) {
        mutex.withLock {
            flow.value = state
            writeCount++
        }
    }

    override suspend fun update(transform: (TimerState) -> TimerState?): Boolean = mutex.withLock {
        val currentState = flow.value
        val next = transform(currentState)
        if (next != null && next != currentState) {
            flow.value = next
            writeCount++
            true
        } else {
            false
        }
    }
}

class FakeSettingsRepository(initial: TimerSettings = TimerSettings()) : SettingsRepository {

    private val flow = MutableStateFlow(initial)

    override val settings: Flow<TimerSettings> = flow.asStateFlow()

    override suspend fun current(): TimerSettings = flow.value

    fun set(settings: TimerSettings) {
        flow.value = settings
    }

    override suspend fun setDurationMinutes(type: SlotType, minutes: Int) {
        val coerced = minutes.coerceIn(TimerSettings.minutesRangeFor(type))
        flow.value = when (type) {
            SlotType.FOCUS -> flow.value.copy(focusMinutes = coerced)
            SlotType.SHORT_BREAK -> flow.value.copy(shortBreakMinutes = coerced)
            SlotType.LONG_BREAK -> flow.value.copy(longBreakMinutes = coerced)
        }
    }

    override suspend fun setPomodorosPerCycle(count: Int) {
        flow.value = flow.value.copy(
            pomodorosPerCycle = count.coerceIn(TimerSettings.POMODOROS_PER_CYCLE_RANGE),
        )
    }

    override suspend fun setAutoStartBreak(enabled: Boolean) {
        flow.value = flow.value.copy(autoStartBreak = enabled)
    }

    override suspend fun setAutoStartFocus(enabled: Boolean) {
        flow.value = flow.value.copy(autoStartFocus = enabled)
    }

    override suspend fun setFocusAlertSound(sound: AlertSound) {
        flow.value = flow.value.copy(focusAlertSound = sound)
    }

    override suspend fun setBreakAlertSound(sound: AlertSound) {
        flow.value = flow.value.copy(breakAlertSound = sound)
    }

    override suspend fun setFocusAlertRepeats(count: Int) {
        flow.value = flow.value.copy(
            focusAlertRepeats = count.coerceIn(TimerSettings.ALERT_REPEATS_RANGE),
        )
    }

    override suspend fun setBreakAlertRepeats(count: Int) {
        flow.value = flow.value.copy(
            breakAlertRepeats = count.coerceIn(TimerSettings.ALERT_REPEATS_RANGE),
        )
    }

    override suspend fun setVibrationSeconds(seconds: Int) {
        flow.value = flow.value.copy(
            vibrationSeconds = seconds.coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE),
        )
    }

    override suspend fun setKeepScreenOn(mode: KeepScreenOnMode) {
        flow.value = flow.value.copy(keepScreenOn = mode)
    }

    override suspend fun setDailyGoal(pomodoros: Int) {
        flow.value = flow.value.copy(dailyGoal = pomodoros.coerceIn(TimerSettings.DAILY_GOAL_RANGE))
    }

    override suspend fun setWidgetBackground(background: WidgetBackground) {
        flow.value = flow.value.copy(widgetBackground = background)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        flow.value = flow.value.copy(language = language)
    }

    override suspend fun setLiquidAnimationEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(liquidAnimationEnabled = enabled)
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        flow.value = flow.value.copy(onboardingDone = done)
    }
}

class FakeStatsRepository : StatsRepository {

    private val rows = MutableStateFlow<List<FocusSession>>(emptyList())

    val recorded: List<FocusSession> get() = rows.value

    /** Stands in for `UNIQUE(session_id, slot_index)` with `OnConflictStrategy.IGNORE`. */
    override suspend fun record(session: FocusSession): Boolean {
        val key = session.sessionId to session.slotIndex
        if (rows.value.any { (it.sessionId to it.slotIndex) == key }) return false
        rows.value = rows.value + session
        return true
    }

    override suspend fun isRecorded(sessionId: Long, slotIndex: Int): Boolean =
        rows.value.any { it.sessionId == sessionId && it.slotIndex == slotIndex }

    override fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayStats>> =
        allDailyTotals().map { days -> days.filter { it.date >= from && it.date <= to } }

    override fun allDailyTotals(): Flow<List<DayStats>> = rows.map { sessions ->
        sessions.groupBy { it.localDate }
            .map { (date, group) ->
                DayStats(
                    date = date,
                    completedPomodoros = group.count { it.completed },
                    focusedMs = group.sumOf { it.actualFocusMs },
                )
            }
            .sortedBy { it.date }
    }

    override fun completedDays(): Flow<List<LocalDate>> = rows.map { sessions ->
        sessions.filter { it.completed }.map { it.localDate }.distinct().sorted()
    }
}

/**
 * Records what the engine asked the runtime to do, so the tests can assert on the service, the alarm
 * and the notification without any Android around.
 */
class FakeTimerRuntime : TimerAlarmScheduler, TimerServiceController, TimerNotifier {

    var armedDeadlineMs: Long? = null
        private set
    var armCount: Int = 0
        private set
    var cancelCount: Int = 0
        private set
    var serviceRunning: Boolean = false
        private set
    var startCount: Int = 0
        private set
    var runningNotificationCount: Int = 0
        private set
    var pausedNotificationCount: Int = 0
        private set
    var finishedNotificationCount: Int = 0
        private set

    /** Of the finished alerts, how many were published for a slot that started by itself. */
    var chainedNotificationCount: Int = 0
        private set

    /** Set to false to simulate `ForegroundServiceStartNotAllowedException`. */
    var serviceStartAllowed: Boolean = true

    /** Set to false to simulate `SCHEDULE_EXACT_ALARM` denied. */
    var exactAlarmsAllowed: Boolean = true

    override fun arm(deadlineElapsedRealtimeMs: Long): Boolean {
        armedDeadlineMs = deadlineElapsedRealtimeMs
        armCount++
        return exactAlarmsAllowed
    }

    override fun cancel() {
        armedDeadlineMs = null
        cancelCount++
    }

    override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed

    override fun start(): Boolean {
        startCount++
        serviceRunning = serviceStartAllowed
        return serviceStartAllowed
    }

    override fun stop() {
        serviceRunning = false
    }

    override fun showRunning(state: TimerState) {
        runningNotificationCount++
    }

    override fun showPaused(state: TimerState, remainingMs: Long) {
        pausedNotificationCount++
    }

    override fun showSlotFinished(state: TimerState, chained: Boolean) {
        finishedNotificationCount++
        if (chained) chainedNotificationCount++
    }

    override fun clearOngoing() = Unit

    override fun clearAlert() = Unit
}

class FakeAlertPlayer : AlertPlayer {

    /** Cada sonido reproducido, en orden: un ciclo alterna el del pomodoro y el del descanso. */
    val playedSounds: MutableList<AlertSound> = mutableListOf()
    var stopCount: Int = 0
        private set
    var lastVibrationSeconds: Int? = null
        private set

    /** Repeticiones pedidas en cada llamada, en el mismo orden que [playedSounds]. */
    val playedRepeats: MutableList<Int> = mutableListOf()

    val playCount: Int get() = playedSounds.size
    val lastSound: AlertSound? get() = playedSounds.lastOrNull()
    val lastRepeats: Int? get() = playedRepeats.lastOrNull()

    override suspend fun play(sound: AlertSound, vibrationSeconds: Int, repeats: Int) {
        playedSounds += sound
        playedRepeats += repeats
        lastVibrationSeconds = vibrationSeconds
    }

    override fun stop() {
        stopCount++
    }
}

/** The charger, plugged and unplugged by hand. */
class FakeChargingMonitor(initial: Boolean = false) : ChargingMonitor {

    private val flow = MutableStateFlow(initial)

    override val isCharging: Flow<Boolean> = flow.asStateFlow()

    fun set(charging: Boolean) {
        flow.value = charging
    }
}

/** A `Clock` whose instant the test moves by hand. */
class MutableClock(
    private var instant: Instant,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    override fun instant(): Instant = instant

    fun advanceBy(millis: Long) {
        instant = instant.plusMillis(millis)
    }

    fun setEpochMillis(epochMillis: Long) {
        instant = Instant.ofEpochMilli(epochMillis)
    }
}

/** The monotonic counterpart, moved independently so clock changes and reboots can be simulated. */
class MutableElapsedRealtime(private var value: Long) : ElapsedRealtimeSource {

    override fun millis(): Long = value

    fun advanceBy(millis: Long) {
        value += millis
    }

    fun setMillis(millis: Long) {
        value = millis
    }
}

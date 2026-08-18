package com.jjrapps.aquihaytomate.domain.model

/**
 * The complete snapshot of the engine. This is layer 1 of the hybrid design: the single source of
 * truth, persisted in `timer_state.preferences_pb` and read by the UI, the widget, the notification
 * and the service alike. **Nothing ticks on it** — the remaining time is always derived from the
 * deadlines by [com.jjrapps.aquihaytomate.domain.usecase.TimerMath].
 *
 * See CLAUDE.md §6 and docs/decisions/002-motor-del-temporizador-hibrido.md.
 *
 * @param sessionId epoch millis at which the current batch started; groups the slots of one run and
 *   doubles as the grouping key in `focus_session`. Zero when no batch has begun.
 * @param slotIndex position of this slot inside the batch. With [sessionId] it forms the unique key
 *   that makes slot completion idempotent.
 * @param completedFocusInCycle focus slots finished *in full* in the current cycle. Skipping a
 *   focus slot does not move it, so skipping never brings the long break closer.
 * @param pomodorosPerCycle frozen from settings when the batch starts, so changing Settings
 *   mid-cycle does not reshape the run already under way.
 * @param slotDurationMs frozen the same way. In [TimerStatus.IDLE] it is stale by design; read
 *   [durationMsWith] instead of this field when a pending slot is being displayed or started.
 * @param endAtEpochMs wall-clock deadline. Used only when a reboot or a clock change is detected.
 * @param endAtElapsedRealtimeMs monotonic deadline: immune to clock changes and it keeps advancing
 *   while the device is suspended. This is the one normally used.
 * @param bootEpochMs `epochNow - elapsedRealtimeNow` at write time. Comparing it against the same
 *   difference computed later is how a reboot or a manual clock change is spotted.
 * @param remainingAtPauseMs what was left when the user paused. Meaningful in [TimerStatus.PAUSED].
 * @param lastActivityEpochMs wall-clock time of the last move of the state machine. It is the only
 *   field that survives across batches, and it exists so a stopped timer can tell whether it belongs
 *   to the day the user is living: see
 *   [com.jjrapps.aquihaytomate.domain.usecase.DayRollover].
 */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val slotType: SlotType = SlotType.FOCUS,
    val sessionId: Long = 0L,
    val slotIndex: Int = 0,
    val completedFocusInCycle: Int = 0,
    val pomodorosPerCycle: Int = TimerSettings.DEFAULT_POMODOROS_PER_CYCLE,
    val slotDurationMs: Long = TimerSettings.DEFAULT_FOCUS_MINUTES * TimerSettings.MINUTE_MS,
    val slotStartedAtEpochMs: Long = 0L,
    val endAtEpochMs: Long = 0L,
    val endAtElapsedRealtimeMs: Long = 0L,
    val bootEpochMs: Long = 0L,
    val remainingAtPauseMs: Long = 0L,
    val lastActivityEpochMs: Long = 0L,
) {

    /**
     * The slot length to honour right now. While a batch is under way this is the frozen
     * [slotDurationMs]; when idle there is nothing to protect, so the live settings win and the
     * user sees the duration they just picked.
     */
    fun durationMsWith(settings: TimerSettings): Long =
        if (status == TimerStatus.IDLE) settings.durationMsFor(slotType) else slotDurationMs

    /** Same reasoning as [durationMsWith], for the cycle length. */
    fun pomodorosPerCycleWith(settings: TimerSettings): Int =
        if (status == TimerStatus.IDLE) settings.pomodorosPerCycle else pomodorosPerCycle

    /**
     * Which pomodoro of the cycle the user is on, 1-based, for the `2/4` readout. During a break it
     * reports the pomodoro just finished rather than the next one, which is what the dots show.
     */
    val cyclePosition: Int
        get() = when {
            slotType.isBreak -> completedFocusInCycle.coerceIn(1, pomodorosPerCycle)
            else -> (completedFocusInCycle + 1).coerceIn(1, pomodorosPerCycle)
        }

    /**
     * Whether SKIP is worth offering.
     *
     * Always with a slot under way; when stopped, only if what waits is a break — skipping a pomodoro
     * that has not started is just not starting it. Lives here rather than in `TimerUiState` because the
     * notification offers the same control and the two must not drift.
     */
    val offersSkip: Boolean
        get() = offersSkip(status, slotType)

    /**
     * Whether a stopped timer still has a batch behind it, and therefore something to show.
     *
     * Zero means nothing has been started since the last fresh day, and a notification offering to start
     * a pomodoro nobody asked for is clutter.
     */
    val hasBatchUnderWay: Boolean
        get() = sessionId != 0L

    /**
     * When the timer was last moved, falling back to the older markers.
     *
     * [lastActivityEpochMs] arrived after 1.3.0, so a state written by an earlier version does not
     * carry it. The fallbacks cover that: [slotStartedAtEpochMs] is set in every state a slot can be
     * paused or waiting in, and [sessionId] is the epoch millis the batch began. Zero means there is
     * no marker at all, and callers must read that as "no idea", never as "the epoch".
     */
    val lastTouchedEpochMs: Long
        get() = when {
            lastActivityEpochMs > 0L -> lastActivityEpochMs
            slotStartedAtEpochMs > 0L -> slotStartedAtEpochMs
            else -> sessionId
        }

    companion object {
        /** The rule behind [offersSkip], callable from a screen state that holds no [TimerState]. */
        fun offersSkip(status: TimerStatus, slotType: SlotType): Boolean =
            status != TimerStatus.IDLE || slotType.isBreak

        /** Nothing has ever run. Used as the DataStore default. */
        val EMPTY = TimerState()

        /** A fresh idle state for the next [type] slot, sized from [settings]. */
        fun idle(
            settings: TimerSettings,
            type: SlotType = SlotType.FOCUS,
            completedFocusInCycle: Int = 0,
            sessionId: Long = 0L,
            slotIndex: Int = 0,
        ) = TimerState(
            status = TimerStatus.IDLE,
            slotType = type,
            sessionId = sessionId,
            slotIndex = slotIndex,
            completedFocusInCycle = completedFocusInCycle,
            pomodorosPerCycle = settings.pomodorosPerCycle,
            slotDurationMs = settings.durationMsFor(type),
        )
    }
}

package com.jjrapps.aquihaytomate.domain.model

/**
 * Everything the user can configure. Defaults and bounds live in the companion object so that the
 * DataStore setters, the duration sheet and the tests all coerce against the same numbers.
 *
 * Durations are stored in minutes because that is the unit the user picks; the engine works in
 * milliseconds and converts through [durationMsFor].
 */
data class TimerSettings(
    val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
    val shortBreakMinutes: Int = DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
    val pomodorosPerCycle: Int = DEFAULT_POMODOROS_PER_CYCLE,
    /**
     * Chain straight into the break when a pomodoro ends. **On by default**: the break is earned and its
     * clock should already be running while you get up, which is how the author uses it.
     */
    val autoStartBreak: Boolean = true,
    /**
     * Chain straight into the next pomodoro when a break ends. Off by default, and deliberately not
     * symmetric with [autoStartBreak]: a break often runs long on purpose.
     */
    val autoStartFocus: Boolean = false,
    /** Sound when a pomodoro ends. Soft by default: finishing one is good news. */
    val focusAlertSound: AlertSound = AlertSound.DEFAULT_FOCUS,
    /** Sound when a break ends. Harder by default: this one has to get you back to work. */
    val breakAlertSound: AlertSound = AlertSound.DEFAULT_BREAK,
    val vibrationSeconds: Int = DEFAULT_VIBRATION_SECONDS,
    /** When the Timer screen holds the display awake. Plugged in by default; see [KeepScreenOnMode]. */
    val keepScreenOn: KeepScreenOnMode = KeepScreenOnMode.DEFAULT,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val widgetBackground: WidgetBackground = WidgetBackground.DEFAULT,
    val language: AppLanguage = AppLanguage.DEFAULT,
    val liquidAnimationEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
) {

    fun durationMinutesFor(type: SlotType): Int = when (type) {
        SlotType.FOCUS -> focusMinutes
        SlotType.SHORT_BREAK -> shortBreakMinutes
        SlotType.LONG_BREAK -> longBreakMinutes
    }

    fun durationMsFor(type: SlotType): Long = durationMinutesFor(type) * MINUTE_MS

    /**
     * The sound for the slot that has just **ended**, not the one coming up.
     *
     * Two sounds rather than one because the two moments say opposite things: a pomodoro ending is a
     * reward and a break ending is an order. With a single sound, whoever wants a gentle chime for the
     * pomodoro gets the same chime asking them to go back to work, which is exactly what nobody notices.
     *
     * Keyed on the slot that ended so `CompleteSlotUseCase` and anything else that alerts cannot
     * disagree, the same reason [autoStartsInto] exists.
     */
    fun alertSoundFor(finishedType: SlotType): AlertSound =
        if (finishedType.isBreak) breakAlertSound else focusAlertSound

    /** Vibration disabled is expressed as zero seconds, not as a separate flag. */
    val vibrationEnabled: Boolean get() = vibrationSeconds > 0

    /**
     * Whether the timer should start [nextType] by itself instead of waiting for the user.
     *
     * Two settings rather than one because the two directions are not the same decision: rolling into the
     * break the moment a pomodoro ends is what you want — the break is earned and its clock should already
     * be running while you get up. Rolling back into work is not, because a break often runs long on
     * purpose. Asked as one switch, whoever wants the first has to give up the second.
     *
     * Keyed on the slot coming up, and used by both `CompleteSlotUseCase` and `SkipSlotUseCase`, so
     * finishing a slot and skipping it cannot disagree.
     */
    fun autoStartsInto(nextType: SlotType): Boolean =
        if (nextType.isBreak) autoStartBreak else autoStartFocus

    companion object {
        const val MINUTE_MS = 60_000L

        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_POMODOROS_PER_CYCLE = 4
        const val DEFAULT_VIBRATION_SECONDS = 5
        const val DEFAULT_DAILY_GOAL = 8

        const val MIN_FOCUS_MINUTES = 1
        const val MAX_FOCUS_MINUTES = 180
        const val MIN_SHORT_BREAK_MINUTES = 1
        const val MAX_SHORT_BREAK_MINUTES = 60
        const val MIN_LONG_BREAK_MINUTES = 1
        const val MAX_LONG_BREAK_MINUTES = 120
        const val MIN_POMODOROS_PER_CYCLE = 2
        const val MAX_POMODOROS_PER_CYCLE = 12
        const val MIN_VIBRATION_SECONDS = 0
        const val MAX_VIBRATION_SECONDS = 30
        const val MIN_DAILY_GOAL = 1
        const val MAX_DAILY_GOAL = 24

        val FOCUS_MINUTES_RANGE = MIN_FOCUS_MINUTES..MAX_FOCUS_MINUTES
        val SHORT_BREAK_MINUTES_RANGE = MIN_SHORT_BREAK_MINUTES..MAX_SHORT_BREAK_MINUTES
        val LONG_BREAK_MINUTES_RANGE = MIN_LONG_BREAK_MINUTES..MAX_LONG_BREAK_MINUTES
        val POMODOROS_PER_CYCLE_RANGE = MIN_POMODOROS_PER_CYCLE..MAX_POMODOROS_PER_CYCLE
        val VIBRATION_SECONDS_RANGE = MIN_VIBRATION_SECONDS..MAX_VIBRATION_SECONDS
        val DAILY_GOAL_RANGE = MIN_DAILY_GOAL..MAX_DAILY_GOAL

        fun minutesRangeFor(type: SlotType): IntRange = when (type) {
            SlotType.FOCUS -> FOCUS_MINUTES_RANGE
            SlotType.SHORT_BREAK -> SHORT_BREAK_MINUTES_RANGE
            SlotType.LONG_BREAK -> LONG_BREAK_MINUTES_RANGE
        }
    }
}

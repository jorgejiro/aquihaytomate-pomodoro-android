package com.jjrapps.aquihaytomate.domain.model

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

/**
 * When the Timer screen holds the display awake.
 *
 * Three states rather than a switch because the interesting answer is the middle one: plugged in, the
 * screen costs nothing to keep on, and watching the figure count down while you work is the whole point
 * of the screen. On battery it is a different trade, and whoever wants it anyway can say so.
 *
 * [id] is the stable key written to DataStore; it survives reordering and renaming the entries.
 */
enum class KeepScreenOnMode(val id: String, @param:StringRes val labelRes: Int) {
    NEVER("never", R.string.keep_screen_on_never),
    WHILE_CHARGING("while_charging", R.string.keep_screen_on_while_charging),
    ALWAYS("always", R.string.keep_screen_on_always),
    ;

    /**
     * Whether the screen should be held awake right now, given whether the phone is plugged in.
     *
     * Deliberately blind to the state of the timer. An idle or ringing pomodoro on screen is still a
     * pomodoro screen the user is looking at, and the previous rule — only while `RUNNING` — meant the
     * display died the moment you paused to answer something, which is precisely when you want to see
     * how much of the break is left.
     */
    fun shouldKeepScreenOn(charging: Boolean): Boolean = when (this) {
        NEVER -> false
        WHILE_CHARGING -> charging
        ALWAYS -> true
    }

    /** True when the answer depends on the charger, and only then is it worth watching for it. */
    val dependsOnCharging: Boolean get() = this == WHILE_CHARGING

    /**
     * What the "screen always on" button of the Timer screen does to this mode.
     *
     * The button is a shortcut to [ALWAYS], not a second switch: one setting answers the question, so the
     * button and Settings can never disagree. Switching it on remembers the mode it replaced, and switching
     * it off gives that one back — whoever had [WHILE_CHARGING] keeps it, and whoever had [NEVER] is not
     * handed the charger rule by surprise. With nothing remembered, as when [ALWAYS] was picked in Settings,
     * it falls back to [DEFAULT].
     *
     * @param restore the mode remembered by the last switch on, if any.
     */
    fun toggleAlways(restore: KeepScreenOnMode?): AlwaysToggle = if (this == ALWAYS) {
        AlwaysToggle(mode = restore?.takeUnless { it == ALWAYS } ?: DEFAULT, restore = null)
    } else {
        AlwaysToggle(mode = ALWAYS, restore = this)
    }

    /** The outcome of [toggleAlways]: the new mode, and the one to remember for switching back. */
    data class AlwaysToggle(val mode: KeepScreenOnMode, val restore: KeepScreenOnMode?)

    companion object {
        /**
         * Plugged in, keep it on.
         *
         * The default the author asked for: the phone sits on the desk on its charger during a working
         * morning, and a timer you have to wake up to read is a timer you stop reading.
         */
        val DEFAULT = WHILE_CHARGING

        fun fromId(id: String?): KeepScreenOnMode = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

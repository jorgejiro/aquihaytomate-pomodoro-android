package com.jjrapps.aquihaytomate.domain.model

import java.time.LocalDate

/**
 * One recorded focus slot. Breaks are never persisted — see
 * docs/decisions/003-solo-se-persisten-los-slots-de-enfoque.md.
 *
 * @param sessionId with [slotIndex], the natural key behind `UNIQUE(session_id, slot_index)`. That
 *   index is what makes the race between the service and the backup alarm harmless: both insert
 *   with `IGNORE` and the loser gets `-1L`.
 * @param actualFocusMs time actually spent focusing, pauses excluded. "Minutes focused" sums this
 *   over every row; "pomodoros completed" counts only the rows with [completed].
 * @param completed true when the slot ran to zero. A skipped or reset slot is stored with false,
 *   and only when it lasted at least [MIN_PARTIAL_MS].
 */
data class FocusSession(
    val id: Long = 0L,
    val sessionId: Long,
    val slotIndex: Int,
    val slotType: SlotType = SlotType.FOCUS,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val plannedDurationMs: Long,
    val actualFocusMs: Long,
    val completed: Boolean,
    val timezoneId: String,
    val localDate: LocalDate,
) {
    companion object {
        /**
         * A partial focus slot shorter than this is discarded instead of stored: an accidental
         * start/stop must not pollute the statistics.
         */
        const val MIN_PARTIAL_MS = 60_000L
    }
}

package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.FocusSession
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Turns a finished or abandoned slot into a history row. The single place that decides what gets
 * recorded, so the rules live here rather than being repeated in four command use cases.
 *
 * - **Only focus slots are stored.** Breaks produce no rows; see
 *   docs/decisions/003-solo-se-persisten-los-slots-de-enfoque.md.
 * - **Partial slots under a minute are dropped.** An accidental start/stop must not pollute the data.
 * - The insert is idempotent through `UNIQUE(session_id, slot_index)`, so calling this twice for the
 *   same slot records it once.
 */
class RecordFocusSlotUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {

    /**
     * @param activeElapsedMs time the clock actually ran, pauses excluded.
     * @return true when **this call** wrote the row. False means either the slot was not worth
     *   recording or the other entrypoint had already recorded it — in both cases the caller must not
     *   treat itself as the one that closed the slot.
     */
    suspend operator fun invoke(
        state: TimerState,
        activeElapsedMs: Long,
        completed: Boolean,
    ): Boolean {
        if (state.slotType != SlotType.FOCUS) return false
        if (!completed && activeElapsedMs < FocusSession.MIN_PARTIAL_MS) return false

        val startedAt = state.slotStartedAtEpochMs.takeIf { it > 0L } ?: clock.millis()
        val zone = clock.zone

        return statsRepository.record(
            FocusSession(
                sessionId = state.sessionId,
                slotIndex = state.slotIndex,
                slotType = SlotType.FOCUS,
                startedAtEpochMs = startedAt,
                endedAtEpochMs = clock.millis(),
                plannedDurationMs = state.slotDurationMs,
                actualFocusMs = activeElapsedMs.coerceAtLeast(0L),
                completed = completed,
                timezoneId = zone.id,
                // Attributed to the day the slot started, so a pomodoro straddling midnight counts
                // once, on the day the user began it.
                localDate = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate(),
            ),
        )
    }
}

package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jjrapps.aquihaytomate.domain.model.FocusSession
import com.jjrapps.aquihaytomate.domain.model.SlotType
import java.time.LocalDate

/**
 * A recorded focus slot. Breaks never reach this table — see
 * docs/decisions/003-solo-se-persisten-los-slots-de-enfoque.md.
 *
 * **The `UNIQUE(session_id, slot_index)` index is architecture, not an optimisation.** It is what
 * makes the race between the foreground service and the backup alarm harmless: both paths insert
 * with `OnConflictStrategy.IGNORE` and whichever arrives second gets `-1L` back. No in-memory flags,
 * no locks. Do not remove it. See CLAUDE.md §5.
 */
@Entity(
    tableName = "focus_session",
    indices = [
        Index(value = ["local_date"]),
        Index(value = ["started_at_epoch_ms"]),
        Index(value = ["session_id", "slot_index"], unique = true),
    ],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Epoch millis at which the batch started; groups the slots of one run. */
    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "slot_index")
    val slotIndex: Int,

    /** Always `FOCUS` in v1. The column is open for breaks in a future version. */
    @ColumnInfo(name = "slot_type")
    val slotType: String,

    @ColumnInfo(name = "started_at_epoch_ms")
    val startedAtEpochMs: Long,

    @ColumnInfo(name = "ended_at_epoch_ms")
    val endedAtEpochMs: Long,

    @ColumnInfo(name = "planned_duration_ms")
    val plannedDurationMs: Long,

    /** Time actually focused, pauses excluded. */
    @ColumnInfo(name = "actual_focus_ms")
    val actualFocusMs: Long,

    @ColumnInfo(name = "completed")
    val completed: Boolean,

    /** For example `Europe/Madrid`. Kept so a past day can be reinterpreted if needed. */
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String,

    /** `YYYY-MM-DD` derived from [startedAtEpochMs] in [timezoneId]; the grouping key for stats. */
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,
)

fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    sessionId = sessionId,
    slotIndex = slotIndex,
    slotType = SlotType.fromName(slotType),
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    plannedDurationMs = plannedDurationMs,
    actualFocusMs = actualFocusMs,
    completed = completed,
    timezoneId = timezoneId,
    localDate = localDate,
)

fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    sessionId = sessionId,
    slotIndex = slotIndex,
    slotType = slotType.name,
    startedAtEpochMs = startedAtEpochMs,
    endedAtEpochMs = endedAtEpochMs,
    plannedDurationMs = plannedDurationMs,
    actualFocusMs = actualFocusMs,
    completed = completed,
    timezoneId = timezoneId,
    localDate = localDate,
)

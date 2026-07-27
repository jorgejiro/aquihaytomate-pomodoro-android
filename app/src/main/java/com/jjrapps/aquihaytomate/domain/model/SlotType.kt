package com.jjrapps.aquihaytomate.domain.model

/**
 * The kind of interval the timer is counting down.
 *
 * The name is persisted as-is in `focus_session.slot_type` and in the timer state, so these
 * constants are part of the storage contract: rename them and old rows stop mapping.
 */
enum class SlotType {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK,
    ;

    val isBreak: Boolean get() = this != FOCUS

    companion object {
        fun fromName(name: String?): SlotType =
            entries.firstOrNull { it.name == name } ?: FOCUS
    }
}

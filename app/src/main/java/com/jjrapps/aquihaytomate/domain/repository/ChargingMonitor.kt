package com.jjrapps.aquihaytomate.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whether the phone is plugged in to an external power source.
 *
 * Behind an interface so the Timer screen's "keep the screen on while charging" rule can be tested with
 * a plain flow, and so the only class that touches `BatteryManager` stays in the data layer.
 */
interface ChargingMonitor {

    /**
     * Emits the current state immediately and then on every connect and disconnect.
     *
     * Cold on purpose: whoever collects it is registering a broadcast receiver for as long as they do,
     * so the subscription is meant to follow the screen that needs it and end with it.
     */
    val isCharging: Flow<Boolean>
}

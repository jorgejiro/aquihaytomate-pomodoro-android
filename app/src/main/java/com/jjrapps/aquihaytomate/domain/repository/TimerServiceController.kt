package com.jjrapps.aquihaytomate.domain.repository

/**
 * Layer 2 of the engine: the foreground service that actually runs the countdown.
 *
 * Behind an interface for two reasons. It keeps `startForegroundService` and its exceptions out of the
 * domain, and it makes the whole mechanism swappable: if Play ever rejected the `specialUse` service
 * type, binding a no-op implementation here ships the app without a service and without touching the
 * domain or the UI. That reversibility is itself part of why the engine is hybrid — see
 * docs/decisions/002-motor-del-temporizador-hibrido.md.
 */
interface TimerServiceController {

    /**
     * Starts the service if it is not already running.
     *
     * @return false when the system refused the start — `ForegroundServiceStartNotAllowedException`,
     *   thrown when there is no exemption because the process is in the background. The caller carries
     *   on: the persisted state plus the backup alarm still deliver the slot.
     */
    fun start(): Boolean

    fun stop()
}

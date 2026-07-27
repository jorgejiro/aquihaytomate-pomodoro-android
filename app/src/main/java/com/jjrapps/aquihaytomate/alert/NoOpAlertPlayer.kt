package com.jjrapps.aquihaytomate.alert

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Placeholder player for F3, when the engine is complete but the sounds and the vibrator are not
 * wired up yet (F5).
 *
 * It exists so that `CompleteSlotUseCase` can call the alert from the start and not have to be
 * rewritten later: F5 swaps this binding for `AlertPlayerImpl` and nothing above it changes. Same
 * trick as `TimerServiceController`, and for the same reason — see the reversibility note in
 * docs/decisions/002-motor-del-temporizador-hibrido.md.
 */
@Singleton
class NoOpAlertPlayer @Inject constructor() : AlertPlayer {

    override suspend fun play(sound: AlertSound, vibrationSeconds: Int) {
        Timber.d("Alert not implemented yet: sound=%s vibration=%ds", sound.id, vibrationSeconds)
    }

    override fun stop() = Unit
}

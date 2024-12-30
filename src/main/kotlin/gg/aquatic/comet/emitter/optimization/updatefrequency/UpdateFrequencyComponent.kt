package gg.aquatic.comet.emitter.optimization.updatefrequency

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData

interface UpdateFrequencyComponent {
    val interpolationDelay: Int
    val interpolationDuration: Int
    fun shouldSendUpdate(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean

    companion object {
        fun default(): UpdateFrequencyComponent {
            return IntervalUpdateFrequencyComponent(1)
        }
    }
}
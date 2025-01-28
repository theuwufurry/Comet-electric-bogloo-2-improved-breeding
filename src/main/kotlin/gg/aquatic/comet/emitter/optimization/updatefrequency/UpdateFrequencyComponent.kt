package gg.aquatic.comet.emitter.optimization.updatefrequency

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData

interface UpdateFrequencyComponent {
    val interpolationDelay: Int
    val initialInterpolationDuration: Int
    fun shouldSendUpdate(otherEmitterData: EmitterData, otherParticleData: ParticleData): UpdateFrequencyResult

    companion object {
        fun default(): UpdateFrequencyComponent {
            return IntervalUpdateFrequencyComponent(1, 0)
        }
    }
}

class UpdateFrequencyResult(val shouldUpdate: Boolean, val interpolationDuration: Int?)
package gg.aquatic.comet.particle.display

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData

interface DisplayComponent {
    fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

interface DisplayData

data class TextDisplayComponent(val string: String) : DisplayData
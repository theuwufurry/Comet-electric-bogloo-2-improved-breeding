package gg.aquatic.comet.particle.display.sprite

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.display.DisplayData

interface SpriteComponent : DisplayComponent {
    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

data class SpriteData(val id: String) : DisplayData
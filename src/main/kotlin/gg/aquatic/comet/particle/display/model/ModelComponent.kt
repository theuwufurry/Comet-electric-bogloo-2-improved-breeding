package gg.aquatic.comet.particle.display.model

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.display.DisplayData

interface ModelComponent : DisplayComponent {
    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

data class ModelData(val item: String, val id: Int) : DisplayData
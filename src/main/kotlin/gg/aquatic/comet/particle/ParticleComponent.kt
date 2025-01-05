package gg.aquatic.comet.particle

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData

interface ParticleComponent : Component {
    fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData)
}
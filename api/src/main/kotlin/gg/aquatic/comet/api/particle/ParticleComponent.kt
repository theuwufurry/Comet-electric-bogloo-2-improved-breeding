package gg.aquatic.comet.api.particle

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData


interface ParticleComponent : Component {
    fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData)
    fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData)
}
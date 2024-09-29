package gg.aquatic.particleemitter.emitter.lifetime

import gg.aquatic.particleemitter.emitter.EmitterData

interface EmitterLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData): Boolean
}
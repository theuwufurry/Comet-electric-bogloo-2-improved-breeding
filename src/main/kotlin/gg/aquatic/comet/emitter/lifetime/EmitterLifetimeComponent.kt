package gg.aquatic.comet.emitter.lifetime

import gg.aquatic.comet.emitter.EmitterData

interface EmitterLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData): Boolean
}
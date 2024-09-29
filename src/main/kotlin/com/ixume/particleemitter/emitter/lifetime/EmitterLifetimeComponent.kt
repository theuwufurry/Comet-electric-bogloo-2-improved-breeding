package com.ixume.particleemitter.emitter.lifetime

import com.ixume.particleemitter.emitter.EmitterData

interface EmitterLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData): Boolean
}
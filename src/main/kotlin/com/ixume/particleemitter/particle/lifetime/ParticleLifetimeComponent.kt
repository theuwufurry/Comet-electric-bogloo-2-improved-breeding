package com.ixume.particleemitter.particle.lifetime

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData

interface ParticleLifetimeComponent {
    fun keepAlive(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean
}
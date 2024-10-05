package com.ixume.particleemitter.particle.lifetime

import com.ixume.particleemitter.particle.ParticleData

interface ParticleLifetimeComponent {
    fun keepAlive(otherParticleData: ParticleData): Boolean
}
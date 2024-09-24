package com.ixume.particlesTesting.emitter.lifetime

import com.ixume.particlesTesting.emitter.Emitter
import com.ixume.particlesTesting.emitter.EmitterMochaData
import com.ixume.particlesTesting.particle.Particle
import com.ixume.particlesTesting.particle.ParticleMochaData

interface LifetimeComponent {
    fun live(emitterData: EmitterMochaData, particleData: ParticleMochaData): Boolean
}
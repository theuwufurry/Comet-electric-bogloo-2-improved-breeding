package com.ixume.particleemitter.emitter.lifetime

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import javax.script.Bindings

interface LifetimeComponent {
    fun keepAlive(emitterData: EmitterData, emitterBindings: Bindings, particleData: ParticleData, particleBindings: Bindings): Boolean
}
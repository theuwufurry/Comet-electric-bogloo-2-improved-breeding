package com.ixume.particleemitter.particle.sprite

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData

interface SpriteComponent {
    fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String
}
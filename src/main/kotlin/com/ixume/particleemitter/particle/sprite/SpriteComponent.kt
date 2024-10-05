package com.ixume.particleemitter.particle.sprite

import com.ixume.particleemitter.particle.ParticleData

interface SpriteComponent {
    fun sprite(otherParticleData: ParticleData): String
}
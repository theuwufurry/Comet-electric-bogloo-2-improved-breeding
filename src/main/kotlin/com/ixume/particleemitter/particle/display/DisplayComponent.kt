package com.ixume.particleemitter.particle.display

import com.ixume.particleemitter.particle.ParticleData

interface DisplayComponent {
    fun display(otherParticleData: ParticleData): DisplayData
}

interface DisplayData
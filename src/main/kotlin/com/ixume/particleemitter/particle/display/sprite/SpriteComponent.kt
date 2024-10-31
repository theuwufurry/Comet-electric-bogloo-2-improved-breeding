package com.ixume.particleemitter.particle.display.sprite

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.display.DisplayData

interface SpriteComponent : DisplayComponent {
    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

data class SpriteData(val id: String) : DisplayData
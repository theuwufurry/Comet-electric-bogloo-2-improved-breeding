package com.ixume.particleemitter.particle.display

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData

interface DisplayComponent {
    fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

interface DisplayData

data class TextDisplayComponent(val string: String) : DisplayData {
}
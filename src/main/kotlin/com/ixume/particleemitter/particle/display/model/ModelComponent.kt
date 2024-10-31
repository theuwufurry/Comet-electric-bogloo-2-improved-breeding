package com.ixume.particleemitter.particle.display.model

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.display.DisplayData

interface ModelComponent : DisplayComponent {
    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData
}

data class ModelData(val item: String, val id: Int) : DisplayData
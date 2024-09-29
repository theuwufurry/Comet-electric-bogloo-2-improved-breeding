package com.ixume.particleemitter.particle.color

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData

fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent {
    fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int
}
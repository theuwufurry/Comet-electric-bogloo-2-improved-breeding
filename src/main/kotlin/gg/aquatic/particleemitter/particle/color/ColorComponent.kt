package gg.aquatic.particleemitter.particle.color

import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.particle.ParticleData

fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent {
    fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int
}
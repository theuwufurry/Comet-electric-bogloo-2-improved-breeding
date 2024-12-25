package gg.aquatic.comet.particle.color

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.particle.ParticleData
import java.awt.Color

fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent {
    fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int
}

fun Color.argb(): Int {
    return rgb + (alpha shl 24)
}
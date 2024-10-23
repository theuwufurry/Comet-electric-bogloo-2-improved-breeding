package com.ixume.particleemitter.particle.color

import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color

fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent {
    fun color(otherParticleData: ParticleData): Int
}

fun Color.argb(): Int {
    return rgb + (alpha shl 24)
}
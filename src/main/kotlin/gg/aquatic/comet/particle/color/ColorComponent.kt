package gg.aquatic.comet.particle.color

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.particleEngine
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

    companion object {
        fun default(): ColorComponent {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ConstantColorComponent(
                engine.compile(("new Color(255, 255, 255, 255)").addDependency()),
                emitterData, particleData
            )
        }
    }
}

fun Color.argb(): Int {
    return rgb + (alpha shl 24)
}
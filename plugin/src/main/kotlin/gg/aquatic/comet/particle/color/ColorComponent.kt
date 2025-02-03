package gg.aquatic.comet.particle.color

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.particleEngine


fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ConstantColorComponent(
                engine.compile(("new Color(255, 255, 255, 255)").addDependency()),
                emitterData, particleData
            )
        }
    }
}
package gg.aquatic.comet.particle.transformation.scale

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.particleEngine

interface ScaleComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)


            return ExpressionScaleComponent(
                engine.compile("1", null),
                engine.compile("1", null),
                engine.compile("1", null),
                particleData, emitterData
            )
        }
    }
}
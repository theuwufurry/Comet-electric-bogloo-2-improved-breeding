package gg.aquatic.comet.particle.transformation.scale

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.particleEngine

interface ScaleComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (_, particleData) = particleEngine(emitterData)


            return ExpressionScaleComponent(
                null, null, null,
                particleData, emitterData
            )
        }
    }
}
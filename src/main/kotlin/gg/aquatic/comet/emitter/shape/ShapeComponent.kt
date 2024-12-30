package gg.aquatic.comet.emitter.shape

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d

    companion object {
        fun default(): ShapeComponent {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            return PointShapeComponent(
                engine.compile("0", null),
                engine.compile("0", null),
                engine.compile("0", null),
                emitterData, particleData
            )
        }
    }
}
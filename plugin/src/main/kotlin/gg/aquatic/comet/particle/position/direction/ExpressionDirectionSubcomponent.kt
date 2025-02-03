package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class ExpressionDirectionSubcomponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    val myParticleData: ParticleData,
    val myEmitterData: EmitterData
) : DirectionSubcomponent {
    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return Vector3d(
            (xOffset.eval() as Number).toDouble(),
            (yOffset.eval() as Number).toDouble(),
            (zOffset.eval() as Number).toDouble()
        )
    }
}

//class UnrealizedExpressionDirectionSubcomponent(private val xOffset: String, private val yOffset: String, private val zOffset: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ExpressionDirectionSubcomponent> {
//    override fun realizeComponent(emitterData: EmitterData): ExpressionDirectionSubcomponent {
//        val (engine, particleData) = particleEngine(emitterData)
//        return ExpressionDirectionSubcomponent(engine.compile(xOffset, macros), engine.compile(yOffset, macros), engine.compile(zOffset, macros), particleData)
//    }
//}
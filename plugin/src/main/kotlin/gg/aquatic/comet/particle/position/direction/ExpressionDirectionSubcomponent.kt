package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3d

class ExpressionDirectionSubcomponent(
    private val xOffset: Expr<Number>?,
    private val yOffset: Expr<Number>?,
    private val zOffset: Expr<Number>?,
    private val magnitude: Expr<Number>?,
    val myParticleData: ParticleData,
    val myEmitterData: EmitterData
) : DirectionSubcomponent {
    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val dir = Vector3d(
            xOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
            yOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
            zOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
        )

        magnitude?.let {
            dir.normalize(it.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 1.0)
        }

        return dir
    }
}
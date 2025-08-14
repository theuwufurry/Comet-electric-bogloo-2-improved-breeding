package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ExpressionPositionComponent(
    private val xOffset: Expr<Number>,
    private val yOffset: Expr<Number>,
    private val zOffset: Expr<Number>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override val priority = 0
    private val cachedOldOutput: MutableMap<UUID, Vector3d> = ConcurrentHashMap()
    private val oldOutput: MutableMap<UUID, Vector3d> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val om = if (otherEmitterData.emitter!!.isPregen) cachedOldOutput else oldOutput

        val oldResult = om.getOrPut(otherParticleData.id) { Vector3d() }
        val newResult = Vector3d(
            (xOffset.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return) * otherEmitterData.emitter!!.environmentData.size,
            (yOffset.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return) * otherEmitterData.emitter!!.environmentData.size,
            (zOffset.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return) * otherEmitterData.emitter!!.environmentData.size,
        )
        om[otherParticleData.id] = newResult.rotate(myEmitterData.emitter!!.pose.rot)
        val newPos = Vector3d(otherParticleData.relativePosition).add(Vector3d(newResult).sub(oldResult))
        otherParticleData.relativePosition = newPos
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "expression_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ExpressionPositionComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(ExpressionPositionComponent(
                jsonObject.getExpr("x")
                    .fold({it}, {return Result.failure(it)})
                    .constructExpr<Number>(engine, macros)
                    .fold({it}, {return Result.failure(it)}),
                jsonObject.getExpr("y")
                    .fold({it}, {return Result.failure(it)})
                    .constructExpr<Number>(engine, macros)
                    .fold({it}, {return Result.failure(it)}),
                jsonObject.getExpr("z")
                    .fold({it}, {return Result.failure(it)})
                    .constructExpr<Number>(engine, macros)
                    .fold({it}, {return Result.failure(it)}),
                emitterData, particleData
            ))
        }
    }
}
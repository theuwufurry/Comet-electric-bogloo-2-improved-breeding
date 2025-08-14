package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3d

class InitialExpressionPositionComponent(
    private val xOffset: Expr<Number>?,
    private val yOffset: Expr<Number>?,
    private val zOffset: Expr<Number>?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override val priority = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            otherParticleData.relativePosition.add(
                Vector3d(
                    xOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
                    yOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
                    zOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 0.0,
                ).rotate(otherEmitterData.emitter!!.pose.rot)
            )
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {

        override val id: String = "initial_expression_position"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<InitialExpressionPositionComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                InitialExpressionPositionComponent(
                    jsonObject.getExprOrNull("x")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("y")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("z")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    emitterData, particleData
                )
            )
        }
    }
}
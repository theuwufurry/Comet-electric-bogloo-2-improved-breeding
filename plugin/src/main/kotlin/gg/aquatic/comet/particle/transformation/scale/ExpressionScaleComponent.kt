package gg.aquatic.comet.particle.transformation.scale

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3f

class ExpressionScaleComponent(
    private val xScale: Expr<Number>?,
    private val yScale: Expr<Number>?,
    private val zScale: Expr<Number>?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, ScaleComponent {
    override val priority = 0

    companion object : BaseComponentParser, ScaleComponent {
        override val id: String = "expression_scale"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<ExpressionScaleComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ExpressionScaleComponent(
                    jsonObject.getExprOrNull("x")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("y")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("z")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    particleData, emitterData
                )
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        otherParticleData.scale = Vector3f(
            (xScale?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (yScale?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (zScale?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
package gg.aquatic.comet.particle.transformation.translation

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

class ExpressionTranslationComponent(
    private val xOffset: Expr<Number>?,
    private val yOffset: Expr<Number>?,
    private val zOffset: Expr<Number>?,
    private val xAbsoluteOffset: Expr<Number>?,
    private val yAbsoluteOffset: Expr<Number>?,
    private val zAbsoluteOffset: Expr<Number>?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData,
) : ParticleComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "expression_translation"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<ExpressionTranslationComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ExpressionTranslationComponent(
                    jsonObject.getExprOrNull("x")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("y")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("z")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("abs_x")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("abs_y")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("abs_z")
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

        otherParticleData.translation = Vector3f(
            (xOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (yOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (zOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
        ).rotate(otherParticleData.rotation).add(
            (xAbsoluteOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (yAbsoluteOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (zAbsoluteOffset?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat()
                ?: 0f) * otherEmitterData.emitter!!.environmentData.size.toFloat(),
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
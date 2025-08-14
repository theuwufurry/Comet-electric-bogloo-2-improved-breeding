package gg.aquatic.comet.particle.transformation.rotation

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
import org.joml.Quaternionf
import org.joml.Vector3f

class DirectionRotationComponent(
    private val axisXScript: Expr<Number>,
    private val axisYScript: Expr<Number>,
    private val axisZScript: Expr<Number>,
    private val angleScript: Expr<Number>,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, RotationComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "direction_rotation"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<DirectionRotationComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                DirectionRotationComponent(
                    jsonObject.getExpr("x")
                        .fold({it}, {return Result.failure(it)})
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExpr("y")
                        .fold({it}, {return Result.failure(it)})
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExpr("z")
                        .fold({it}, {return Result.failure(it)})
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExpr("angle")
                        .fold({it}, {return Result.failure(it)})
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    particleData, emitterData
                )
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val dir = Vector3f(
            axisXScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return,
            axisYScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return,
            axisZScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return,
        ).normalize()

        otherParticleData.rotation = otherEmitterData.emitter!!.applyEmitterRotation(
            Quaternionf()
                .rotateTo(Vector3f(0f, 0f, 1f), dir)
                .rotateZ(angleScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: return)
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
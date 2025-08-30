package gg.aquatic.comet.particle.color

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
import java.awt.Color

class ConstantColorComponent(
    private val colorScript: Expr<Color>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, ColorComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "constant_color"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ConstantColorComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ConstantColorComponent(
                    jsonObject.getExpr("color")
                        .fold({ it }, { return Result.failure(it) })
                        .addDependency()
                        .constructExpr<Color>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    emitterData, particleData
                )
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        otherParticleData.color = if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            colorScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.rgb ?: otherParticleData.color
        } else {
            otherParticleData.color
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
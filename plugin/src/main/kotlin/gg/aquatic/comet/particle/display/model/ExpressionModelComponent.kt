package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.model.ModelComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ExpressionModelComponent(
    private val id: Expr<String>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, ModelComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "expression_model"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(ExpressionModelComponent(
                jsonObject.getExpr("id")
                    .fold({ it }, { return Result.failure(it) })
                    .constructExpr<String>(engine, macros, tryAsSimpleString = true)
                    .fold({ it }, { return Result.failure(it) }),
                emitterData,
                particleData
            ))
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.displayData = run {
            val id = id.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
            if (id !in ResourcepackCreator.modelMap.keys && id != "empty") {
                AbstractParticleEmitter.INSTANCE.logger.warning("Invalid model id $id!")
                return
            }

            ModelData(id)
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
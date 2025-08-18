package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.model.ModelComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class FlipbookModelComponent(
    private val inputScript: Expr<Number>,
    private val modelScripts: List<Pair<Double, Expr<String>>>,
    private val myParticleData: ParticleData, private val myEmitterData: EmitterData
) : ParticleComponent, ModelComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "flipbook_model"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val models: MutableList<Pair<Double, Expr<String>>> = mutableListOf()
            if (jsonObject["models"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("models")) {
                    models += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (
                            element.getExpr("model")
                                .fold({ it }, { return Result.failure(it) })
                                .constructExpr<String>(engine, macros)
                                .fold({ it }, { return Result.failure(it) })
                            )
                }
            } else if (jsonObject["models"].isJsonObject) {
                for ((index, model) in jsonObject["models"].asJsonObject.entrySet()) {
                    models += index.toDouble() to (
                            model.asStringOrNull()
                                ?.constructExpr<String>(engine, macros)
                                ?.fold({ it }, { return Result.failure(it) })
                                ?: return Result.failure(InvalidJsonException("Missing model!")))
                }
            } else {
                return Result.failure(InvalidJsonException("Malformed element!"))
            }


            return Result.success(FlipbookModelComponent(
                jsonObject.getExpr("input")
                    .fold({ it }, { return Result.failure(it) })
                    .constructExpr<Number>(engine, macros)
                    .fold({ it }, { return Result.failure(it) }),
                models,
                particleData, emitterData
            ))
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val inputResult = inputScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return

        if (inputResult >= modelScripts.last().first) {
            otherParticleData.displayData = ModelData(modelScripts.last().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
            return
        }

        if (inputResult <= modelScripts.first().first) {
            otherParticleData.displayData = ModelData(modelScripts.first().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
            return
        }

        var i = 0
        while (i + 1 < modelScripts.size && inputResult > modelScripts[i + 1].first) i++
        otherParticleData.displayData = ModelData(modelScripts[i].second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
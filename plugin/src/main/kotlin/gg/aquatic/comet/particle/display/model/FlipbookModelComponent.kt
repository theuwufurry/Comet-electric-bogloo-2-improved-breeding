package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.model.ModelComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class FlipbookModelComponent(
    private val inputScript: CompiledScript,
    private val modelScripts: List<Pair<Double, CompiledScript>>,
    private val myParticleData: ParticleData, private val myEmitterData: EmitterData
) : ParticleComponent, ModelComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "flipbook_model"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val models: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            if (jsonObject["models"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("models")) {
                    models += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (engine.compile(
                        element.expression("model") ?: return null,
                        macros, true
                    ) ?: continue)
                }
            } else if (jsonObject["models"].isJsonObject) {
                for ((index, model) in jsonObject["models"].asJsonObject.entrySet()) {
                    models += index.toDouble() to (engine.compile(
                        model.asStringOrNull() ?: return null,
                        macros, true
                    ) ?: continue)
                }
            } else {
                return null
            }


            return FlipbookModelComponent(
                engine.compile(jsonObject.expression("input") ?: return null, macros) ?: return null,
                models,
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val inputResult = (inputScript.eval() as Number).toDouble()

        if (inputResult >= modelScripts.last().first) {
            otherParticleData.displayData = ModelData(modelScripts.last().second.eval() as String)
            return
        }

        if (inputResult <= modelScripts.first().first) {
            otherParticleData.displayData = ModelData(modelScripts.first().second.eval() as String)
            return
        }

        var i = 0
        while (i + 1 < modelScripts.size && inputResult > modelScripts[i + 1].first) i++
        otherParticleData.displayData = ModelData(modelScripts[i].second.eval() as String)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.model.ModelComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ConstantModelComponent(
    private val item: CompiledScript,
    private val id: CompiledScript,
    private val myEmitterData: EmitterData
) : ParticleComponent, ModelComponent {
    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "constant_model" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantModelComponent(
                engine.compile(jsonObject.expression("item") ?: return null, macros),
                engine.compile(jsonObject.expression("id") ?: return null, macros),
                emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            ModelData(item.eval() as String, (id.eval() as Number).toInt())
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
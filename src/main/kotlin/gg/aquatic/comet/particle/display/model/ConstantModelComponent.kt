package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.display.DisplayData
import javax.script.CompiledScript

class ConstantModelComponent(
    private val item: CompiledScript,
    private val id: CompiledScript,
    private val myEmitterData: EmitterData
) : ModelComponent {
    companion object : ComponentParser<ConstantModelComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "constant_model" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantModelComponent? {
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

    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData {
        myEmitterData.copyFrom(otherEmitterData)
        return if (otherParticleData.age == 0.0) {
            ModelData(item.eval() as String, id.eval() as Int)
        } else {
            otherParticleData.displayData
        }
    }
}
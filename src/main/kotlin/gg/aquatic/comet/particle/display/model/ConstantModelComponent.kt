package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
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
}
package gg.aquatic.comet.particle.display.model

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.model.ModelComponent
import gg.aquatic.comet.api.particle.display.model.ModelData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ConstantModelComponent(
    private val id: CompiledScript,
    private val myEmitterData: EmitterData
) : ParticleComponent, ModelComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "constant_model"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantModelComponent(
                engine.compile(jsonObject.expression("id") ?: return null, macros, true),
                emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            val id = id.eval() as String
            if (id !in ResourcepackCreator.modelMap.keys) {
                AbstractParticleEmitter.INSTANCE.logger.warning("Invalid model id $id!")
                return
            }

            ModelData(id)
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
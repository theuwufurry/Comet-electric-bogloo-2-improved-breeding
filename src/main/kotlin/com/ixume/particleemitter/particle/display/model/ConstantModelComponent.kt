package com.ixume.particleemitter.particle.display.model

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.display.DisplayData
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
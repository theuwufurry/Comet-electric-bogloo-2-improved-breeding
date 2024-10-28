package com.ixume.particleemitter.particle.display.model

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.display.DisplayData
import javax.script.CompiledScript

class ConstantModelComponent(private val item: CompiledScript, private val id: CompiledScript) : ModelComponent {
    companion object : ComponentParser<ConstantModelComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "constant_model" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ConstantModelComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedConstantModelComponent(
                jsonObject.expression("item") ?: return null,
                jsonObject.expression("id") ?: return null,
                macros
            )
        }
    }

    override fun display(otherParticleData: ParticleData): DisplayData {
        return if (otherParticleData.age == 0.0) {
            ModelData(item.eval() as String, id.eval() as Int)
        } else {
            otherParticleData.displayData
        }
    }
}

class UnrealizedConstantModelComponent(private val item: String, private val id: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ConstantModelComponent> {
    override fun realizeComponent(emitterData: EmitterData): ConstantModelComponent {
        val engine = emitterEngine(emitterData)
        return ConstantModelComponent(engine.compile(item, macros), engine.compile(id, macros))
    }
}
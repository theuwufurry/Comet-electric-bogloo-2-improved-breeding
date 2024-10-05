package com.ixume.particleemitter.particle.color

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class ConstantColorComponent(private val colorScript: CompiledScript) : ColorComponent {
    companion object : ComponentParser<ConstantColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "constant_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ConstantColorComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedConstantColorComponent(
                jsonObject.expression("color")?.addDependency() ?: return null,
                macros
            )
        }
    }

    override fun color(otherParticleData: ParticleData): Int {
        return if (otherParticleData.age == 0.0) {
            (colorScript.eval() as Color).rgb
        } else {
            otherParticleData.color
        }
    }
}

class UnrealizedConstantColorComponent(private val color: String, private val macros: Map<String, Macro>?): UnrealizedComponent<ConstantColorComponent> {
    override fun realizeComponent(emitterData: EmitterData): ConstantColorComponent {
        val engine = emitterEngine(emitterData)
        return ConstantColorComponent(engine.compile(color, macros))
    }
}
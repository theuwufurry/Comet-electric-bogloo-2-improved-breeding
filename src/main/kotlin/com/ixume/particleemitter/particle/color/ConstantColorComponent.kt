package com.ixume.particleemitter.particle.color

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class ConstantColorComponent(private val colorScript: CompiledScript, private val myEmitterData: EmitterData) : ColorComponent {
    companion object : ComponentParser<ColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "constant_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantColorComponent? {
            val (engine, emitterData) = emitterEngine()
            val jsonObject = jsonElement.asJsonObject
            return ConstantColorComponent(
                engine.compile(jsonObject.expression("color")?.addDependency() ?: return null, macros),
                emitterData
            )
        }
    }

    override fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int {
        return if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            (colorScript.eval() as Color).rgb
        } else {
            otherParticleData.color
        }
    }
}
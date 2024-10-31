package com.ixume.particleemitter.particle.display.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.display.DisplayData
import javax.script.CompiledScript

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) :
    SpriteComponent {
    companion object : ComponentParser<ConstantSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros),
                emitterData
            )
        }
    }

    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData {
        myEmitterData.copyFrom(otherEmitterData)
        return if (otherParticleData.age == 0.0) {
            SpriteData(sprite.eval() as String)
        } else {
            otherParticleData.displayData
        }
    }
}
package com.ixume.particleemitter.particle.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) : SpriteComponent {
    companion object : ComponentParser<SpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantSpriteComponent? {
            val (engine, emitterData) = emitterEngine()
            val jsonObject = jsonElement.asJsonObject
            return ConstantSpriteComponent(
                jsonObject.expression("sprite")?.let {
                    engine.compile(it, macros) } ?: return null,
                emitterData
            )
        }
    }

    override fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String {
        return if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            sprite.eval() as String
        } else {
            otherParticleData.sprite
        }
    }
}
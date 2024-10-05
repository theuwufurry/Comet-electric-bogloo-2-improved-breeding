package com.ixume.particleemitter.particle.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ConstantSpriteComponent(private val sprite: CompiledScript) : SpriteComponent {
    companion object : ComponentParser<ConstantSpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ConstantSpriteComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedConstantSpriteComponent(
                jsonObject.expression("sprite") ?: return null,
                macros
            )
        }
    }

    override fun sprite(otherParticleData: ParticleData): String {
        return if (otherParticleData.age == 0.0) {
            sprite.eval() as String
        } else {
            otherParticleData.sprite
        }
    }
}

class UnrealizedConstantSpriteComponent(private val sprite: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ConstantSpriteComponent> {
    override fun realizeComponent(emitterData: EmitterData): ConstantSpriteComponent {
        val engine = emitterEngine(emitterData)
        return ConstantSpriteComponent(engine.compile(sprite, macros))
    }
}
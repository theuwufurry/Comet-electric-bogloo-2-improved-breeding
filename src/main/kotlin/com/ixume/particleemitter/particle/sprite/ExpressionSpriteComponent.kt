package com.ixume.particleemitter.particle.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ExpressionSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : SpriteComponent {
    companion object : ComponentParser<SpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "expression_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionSpriteComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject
            return ExpressionSpriteComponent(
                jsonObject.expression("sprite")?.let {
                    engine.compile(it, macros) } ?: return null,
                emitterData,
                particleData
            )
        }
    }

    override fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return sprite.eval() as String
    }
}
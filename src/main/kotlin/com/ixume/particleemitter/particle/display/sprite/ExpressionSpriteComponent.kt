package com.ixume.particleemitter.particle.display.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ExpressionSpriteComponent(
    private val sprite: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : SpriteComponent {
    companion object : ComponentParser<ExpressionSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "expression_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros),
                particleData, emitterData
            )
        }
    }

    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): SpriteData {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return SpriteData(sprite.eval() as String)
    }
}
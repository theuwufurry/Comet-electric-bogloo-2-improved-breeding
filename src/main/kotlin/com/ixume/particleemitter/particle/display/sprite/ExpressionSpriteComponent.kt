package com.ixume.particleemitter.particle.display.sprite

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ExpressionSpriteComponent(private val sprite: CompiledScript, private val myParticleData: ParticleData) : SpriteComponent {
    companion object : ComponentParser<ExpressionSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "expression_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ExpressionSpriteComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedExpressionSpriteComponent(
                jsonObject.expression("sprite") ?: return null,
                macros
            )
        }
    }

    override fun display(otherParticleData: ParticleData): SpriteData {
        myParticleData.copyFrom(otherParticleData)
        return SpriteData(sprite.eval() as String)
    }
}

class UnrealizedExpressionSpriteComponent(private val sprite: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ExpressionSpriteComponent> {
    override fun realizeComponent(emitterData: EmitterData): ExpressionSpriteComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ExpressionSpriteComponent(engine.compile(sprite, macros), particleData)
    }
}
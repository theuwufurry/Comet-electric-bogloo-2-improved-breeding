package com.ixume.particleemitter.particle.texture

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.expression
import com.ixume.particleemitter.particle.ParticleData
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) : SpriteComponent {
    companion object : ComponentParser<SpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement): ConstantSpriteComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData(0.0)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return ConstantSpriteComponent(
                jsonObject.expression("sprite")?.let {
                    (engine as Compilable).compile(it) } ?: return null,
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
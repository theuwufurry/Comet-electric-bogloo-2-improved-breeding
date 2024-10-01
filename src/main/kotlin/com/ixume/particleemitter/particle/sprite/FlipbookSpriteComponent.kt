package com.ixume.particleemitter.particle.sprite

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class FlipbookSpriteComponent(private val inputScript: CompiledScript,
                              private val spriteScripts: List<Pair<Double, CompiledScript>>,
                              private val myEmitterData: EmitterData,
                              private val myParticleData: ParticleData) : SpriteComponent {
    companion object : ComponentParser<SpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "flipbook_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpriteComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject
            val sprites: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("sprites")) {
                sprites += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to engine.compile(element.expression("sprite") ?: return null, macros)
            }

            return FlipbookSpriteComponent(
                engine.compile(jsonObject.expression("input") ?: return null, macros),
                sprites,
                emitterData,
                particleData
            )
        }
    }

    override fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val inputResult = inputScript.eval() as Double

        var i = 0
        while (i + 1 < spriteScripts.size && inputResult >= spriteScripts[i + 1].first) i++
        return spriteScripts[i].second.eval() as String
    }
}
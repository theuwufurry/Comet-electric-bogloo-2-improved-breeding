package com.ixume.particleemitter.particle.display.sprite

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class FlipbookSpriteComponent(private val inputScript: CompiledScript,
                              private val spriteScripts: List<Pair<Double, CompiledScript>>,
                              private val myParticleData: ParticleData) : SpriteComponent {
    companion object : ComponentParser<FlipbookSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "flipbook_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<FlipbookSpriteComponent>? {
            val jsonObject = jsonElement.asJsonObject
            val sprites: MutableList<Pair<Double, String>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("sprites")) {
                sprites += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (element.expression("sprite") ?: return null)
            }

            return UnrealizedFlipbookSpriteComponent(
                jsonObject.expression("input") ?: return null,
                sprites,
                macros
            )
        }
    }

    override fun display(otherParticleData: ParticleData): SpriteData {
        myParticleData.copyFrom(otherParticleData)
        val inputResult = inputScript.eval() as Double

        var i = 0
        while (i + 1 < spriteScripts.size && inputResult >= spriteScripts[i + 1].first) i++
        return SpriteData(spriteScripts[i].second.eval() as String)
    }
}

class UnrealizedFlipbookSpriteComponent(private val input: String, private val sprites: List<Pair<Double, String>>, private val macros: Map<String, Macro>?) : UnrealizedComponent<FlipbookSpriteComponent> {
    override fun realizeComponent(emitterData: EmitterData): FlipbookSpriteComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return FlipbookSpriteComponent(
            engine.compile(input, macros),
            sprites.map { Pair(it.first, engine.compile(it.second, macros)) },
            particleData
        )
    }
}
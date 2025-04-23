package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class FlipbookSpriteComponent(
    private val inputScript: CompiledScript,
    private val spriteScripts: List<Pair<Double, CompiledScript>>,
    private val myParticleData: ParticleData, private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "flipbook_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): FlipbookSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val sprites: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            if (jsonObject["sprites"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("sprites")) {
                    sprites += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to engine.compile(
                        element.expression("sprite") ?: return null,
                        macros, true
                    )
                }
            } else if (jsonObject["sprites"].isJsonObject) {
                for ((index, sprite) in jsonObject["sprites"].asJsonObject.entrySet()) {
                    sprites += index.toDouble() to engine.compile(
                        sprite.asStringOrNull() ?: return null,
                        macros, true
                    )
                }
            } else {
                return null
            }

            return FlipbookSpriteComponent(
                engine.compile(jsonObject.expression("input") ?: return null, macros),
                sprites,
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val inputResult = (inputScript.eval() as Number).toDouble()

        if (inputResult >= spriteScripts.last().first) {
            otherParticleData.displayData = SpriteData(spriteScripts.last().second.eval() as String)
            return
        }

        if (inputResult <= spriteScripts.first().first) {
            otherParticleData.displayData = SpriteData(spriteScripts.first().second.eval() as String)
            return
        }

        var i = 0
        while (i + 1 < spriteScripts.size && inputResult > spriteScripts[i + 1].first) i++
        otherParticleData.displayData = SpriteData(spriteScripts[i].second.eval() as String)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
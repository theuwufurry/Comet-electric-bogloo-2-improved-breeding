package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class FlipbookSpriteComponent(
    private val inputScript: CompiledScript,
    private val spriteScripts: List<Pair<Double, CompiledScript>>,
    private val myParticleData: ParticleData, private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "flipbook_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): FlipbookSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val sprites: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("sprites")) {
                sprites += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to engine.compile(
                    element.expression("sprite") ?: return null,
                    macros, true
                )
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

        var i = 0
        while (i + 1 < spriteScripts.size && inputResult >= spriteScripts[i + 1].first) i++
        otherParticleData.displayData = SpriteData(spriteScripts[i].second.eval() as String)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}
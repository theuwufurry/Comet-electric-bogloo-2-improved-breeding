package gg.aquatic.comet.particle.action.event

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.parsing.ParticleJsonParser

class ParticleTimelineComponent(
    private val actionMap: Map<Int, Action>
) : ParticleComponent, PostInit {
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        actionMap[otherParticleData.age.toInt()]?.execute(
            ActionContext(
                otherEmitterData, otherParticleData,
                otherParticleData.particle!!.pose
            )
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    override fun realize() {
        actionMap.values.forEach { action ->
            action.subActions.filterIsInstance<PostInit>().forEach { subAction -> subAction.realize() }
        }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_particle_timeline" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            val jsonObject =
                if (jsonElement.isJsonObject) jsonElement.asJsonObject else throw MalformedJsonException("Malformed particle timeline component.")

            val map: MutableMap<Int, Action> = mutableMapOf()

            for ((entry, element) in jsonObject.entrySet()) {
                val time = entry.toIntOrNull() ?: continue
                if (!element.isJsonArray) continue

                map[time] = Action.parse(element.asJsonArray, macros)
            }

            return ParticleTimelineComponent(map)
        }
    }
}
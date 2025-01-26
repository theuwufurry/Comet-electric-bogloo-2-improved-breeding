package gg.aquatic.comet.particle.action.event

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.PostInit
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

class ParticleTimelineComponent(
    private val actionMap: Map<Int, Action>
) : ParticleComponent, PostInit {
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        actionMap[otherParticleData.age.toInt()]?.execute(
            ActionContext(
                otherEmitterData, otherParticleData,
                otherEmitterData.location.toVector().toVector3d(),
                Vector3d()
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
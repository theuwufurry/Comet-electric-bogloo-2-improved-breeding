package gg.aquatic.comet.emitter.action.event

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.PostInit
import gg.aquatic.comet.parsing.macro.Macro

class EmitterTimelineComponent(
    private val actionMap: Map<Int, Action>
) : EmitterComponent, PostInit {
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        actionMap[otherEmitterData.age.toInt()]?.execute(
            ActionContext(
                otherEmitterData, null,
                otherEmitterData.emitter!!.pose()
            )
        )
    }

    override fun die(otherEmitterData: EmitterData) {}

    override fun realize() {
        actionMap.values.forEach { action ->
            action.subActions.filterIsInstance<PostInit>().forEach { subAction -> subAction.realize() }
        }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_emitter_timeline" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            val jsonObject =
                if (jsonElement.isJsonObject) jsonElement.asJsonObject else throw MalformedJsonException("Malformed emitter timeline component.")

            val map: MutableMap<Int, Action> = mutableMapOf()

            for ((entry, element) in jsonObject.entrySet()) {
                val time = entry.toIntOrNull() ?: continue
                if (!element.isJsonArray) continue

                map[time] = Action.parse(element.asJsonArray, macros)
            }

            return EmitterTimelineComponent(map)
        }
    }
}
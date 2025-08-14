package gg.aquatic.comet.emitter.action.event

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.action.Action

class EmitterTimelineComponent(
    private val actionMap: Map<Int, Action>
) : EmitterComponent, PostInit {
    override val priority = 0
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        actionMap[otherEmitterData.age.toInt()]?.execute(
            ActionContext(
                otherEmitterData, null,
                otherEmitterData.emitter!!.pose
            )
        )
    }

    override fun die(otherEmitterData: EmitterData) {}

    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        actionMap.values.forEach { action ->
            action.subActions.filterIsInstance<PostInit>().forEach { subAction -> subAction.realize(unrealizedEmitter) }
        }
    }

    companion object : BaseComponentParser {
        override val id: String = "on_emitter_timeline"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            val jsonObject =
                if (jsonElement.isJsonObject) jsonElement.asJsonObject else throw MalformedJsonException("Malformed emitter timeline component.")

            val map: MutableMap<Int, Action> = mutableMapOf()

            for ((entry, element) in jsonObject.entrySet()) {
                val time = entry.toIntOrNull() ?: continue
                if (!element.isJsonArray) continue

                map[time] = Action.parse(element.asJsonArray, macros).fold(
                    { it },
                    { return Result.failure(it) }
                )
            }

            return Result.success(EmitterTimelineComponent(map))
        }
    }
}
package gg.aquatic.comet.emitter.action

import com.google.gson.JsonArray
import gg.aquatic.comet.emitter.action.sub.ParticleDieSubAction
import gg.aquatic.comet.emitter.action.sub.SpawnEmitterSubAction
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.macro.Macro

class Action(
    val subActions: List<SubAction>
) {
    companion object {
        private val subActionParsers: List<ComponentParser<out SubAction>> = listOf(
            SpawnEmitterSubAction,
            ParticleDieSubAction
        )

        fun parse(jsonArray: JsonArray, macros: Map<String, Macro>?): Action {
            val subActions: MutableList<SubAction> = mutableListOf()
            for (element in jsonArray) {
                subActionParsers.forEach { parser ->
                    parser.parse(element, macros)?.let l@{
                        subActions.add(it)
                        return@l
                    }
                }
            }

            return Action(subActions)
        }
    }

    fun execute(context: ActionContext) {
        subActions.forEach { it.execute(context) }
    }
}

interface SubAction {
    fun execute(context: ActionContext)
}

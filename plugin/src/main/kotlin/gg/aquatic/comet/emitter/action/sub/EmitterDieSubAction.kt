package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.macro.Macro

class EmitterDieSubAction : SubAction {
    override fun execute(context: ActionContext) {
        context.otherEmitterData.dead = true
    }

    companion object : ComponentParser<EmitterDieSubAction> {
        override val id: String = "emitter_die"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<EmitterDieSubAction> {
            if (jsonElement.isJsonPrimitive && jsonElement.asString == "emitter_die") {
                return Result.success(EmitterDieSubAction())
            }

            return Result.failure(NotMyType())
        }
    }
}
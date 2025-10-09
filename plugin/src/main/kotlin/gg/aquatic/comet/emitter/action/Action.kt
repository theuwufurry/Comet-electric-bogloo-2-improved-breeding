package gg.aquatic.comet.emitter.action

import com.google.gson.JsonArray
import gg.aquatic.comet.api.emitter.action.AbstractAction
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.action.sub.EmitterDieSubAction
import gg.aquatic.comet.emitter.action.sub.JavascriptSubAction
import gg.aquatic.comet.emitter.action.sub.SpawnEmitterSubAction
import gg.aquatic.comet.particle.action.ParticleCommandSubAction
import gg.aquatic.comet.particle.action.ParticleDieSubAction
import gg.aquatic.comet.particle.action.ParticleVanillaSpawnSubAction
import gg.aquatic.comet.particle.action.SoundSubAction

class Action(
    override val subActions: List<SubAction>,
) : AbstractAction() {
    companion object {
        private val subActionParsers: MutableList<ComponentParser<out SubAction>> = mutableListOf(
            SpawnEmitterSubAction,
            ParticleDieSubAction,
            EmitterDieSubAction,
            JavascriptSubAction,
            ParticleCommandSubAction,
            ParticleVanillaSpawnSubAction,
            SoundSubAction
        )

        private val parserMap = subActionParsers.associateBy { it.id }

        fun parse(jsonArray: JsonArray, macros: Map<String, Macro>?): Result<Action> {
            val subActions: MutableList<SubAction> = mutableListOf()
            for (element in jsonArray) {
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    obj.get("action_id")?.let { id ->
                        if (id.isJsonPrimitive && id.asJsonPrimitive.isString) {
                            println("action_id: $id")
                            (parserMap[id.asJsonPrimitive.asString]
                             ?: return Result.failure(InvalidJsonException("No action has the id $id!")))
                                .parse(element, macros)
                                .fold(
                                    {
                                        subActions += it
                                        continue
                                    },
                                    {
                                        return Result.failure(it)
                                    }
                                )
                        } else {
                            return Result.failure(InvalidJsonException("Expected action_id to be a string!"))
                        }
                    }
                }

                subActionParsers.forEach { parser ->
                    parser.parse(element, macros).fold(
                        { subActions += it },
                        {
                            if (it !is NotMyType) {
                                return Result.failure(it)
                            }
                        }
                    )
                }
            }

            return Result.success(Action(subActions))
        }
    }

    override fun execute(context: ActionContext) {
        subActions.forEach { it.execute(context) }
    }
}
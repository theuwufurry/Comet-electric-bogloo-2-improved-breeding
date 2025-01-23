package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.parent.EmitterSpace
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import org.bukkit.Location
import org.bukkit.util.Vector

/*
emitter can be in:
- world space, same as regular
- parent space, follows parent particle OR parent emitter?
    - emitter space - available in all contexts, binds to the emitter that spawned it
    - particle space - available in particle context, binds to particle

    pass Parent interface with a getLocation type function
 */

class SpawnEmitterSubAction(
    private val unrealizedEmitterIDs: List<String>,
    private val space: EmitterSpace,
    private val magnitude: Float,
) : SubAction, PostInit {
    private lateinit var unrealizedEmitters: List<UnrealizedEmitter>

    override fun realize() {
        unrealizedEmitters = unrealizedEmitterIDs.map {
            ParticleJsonParser.jsonUnrealizedEmitters[it]
                ?: throw NullPointerException("$it is not a valid emitter ID!")
        }
    }

    override fun execute(context: ActionContext) {
        if (context.pos == null || context.dir == null) {
            throw NullPointerException("Cannot use Spawn Emitter SubAction in this event!")
        }

        val parent = when (space) {
            EmitterSpace.PARENT_EMITTER -> context.otherEmitterData.emitter!!
            EmitterSpace.PARENT_PARTICLE -> context.otherParticleData?.particle
            else -> null
        }

        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.realize(
                parent,
                Location(
                    context.otherEmitterData.world,
                    context.pos.x + context.dir.x * Math.random() * magnitude,
                    context.pos.y + context.dir.y * Math.random() * magnitude,
                    context.pos.z + context.dir.z * Math.random() * magnitude
                ).setDirection(Vector(context.dir.x, context.dir.y, context.dir.z))
            )
        }
    }

    companion object : ComponentParser<SpawnEmitterSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpawnEmitterSubAction? {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.has("emitter"))) return null
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val unrealizedEmitterIDs = if (jsonObject.get("emitter").isJsonPrimitive) {
                listOf(engine.compile(jsonObject.expression("emitter") ?: return null, macros, true).eval() as String)
            } else {
                val arr = jsonObject.getAsJsonArray("emitter")
                val ids: MutableList<String> = mutableListOf()
                for (elem in arr) {
                    ids += engine.compile(elem.asString, macros, true).eval() as String
                }

                ids
            }

            val offsetMagnitude = jsonObject.getAsJsonPrimitive("offset")?.asNumber?.toFloat() ?: 0f
            val space = when (jsonObject.getAsJsonPrimitive("space")?.asString) {
                "parent_emitter" -> EmitterSpace.PARENT_EMITTER
                "parent_particle" -> EmitterSpace.PARENT_PARTICLE
                else -> EmitterSpace.WORLD
            }

            return SpawnEmitterSubAction(unrealizedEmitterIDs, space, offsetMagnitude)
        }
    }
}
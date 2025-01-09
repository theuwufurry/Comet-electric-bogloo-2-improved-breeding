package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import org.bukkit.Location
import org.bukkit.util.Vector

class SpawnEmitterSubAction(
    private val unrealizedEmitterIDs: List<String>,
    private val magnitude: Float,
    private val myEmitterData: EmitterData
) : SubAction, PostInit {
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

            return SpawnEmitterSubAction(unrealizedEmitterIDs, offsetMagnitude, emitterData)
        }
    }

    private lateinit var unrealizedEmitters: List<UnrealizedEmitter>

    override fun realize() {
        unrealizedEmitters = unrealizedEmitterIDs.map { ParticleJsonParser.jsonUnrealizedEmitters[it]!! }
    }

    override fun execute(context: ActionContext) {
        if (context.pos == null || context.dir == null) {
            throw NullPointerException("Cannot use Spawn Emitter SubAction in this event!")
        }

        myEmitterData.copyFrom(context.otherEmitterData)

        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.realize(
                Location(
                    context.otherEmitterData.world,
                    context.pos.x + context.dir.x * Math.random() * magnitude,
                    context.pos.y + context.dir.y * Math.random() * magnitude,
                    context.pos.z + context.dir.z * Math.random() * magnitude
                ).setDirection(Vector(context.dir.x, context.dir.y, context.dir.z))
            )
        }
    }
}
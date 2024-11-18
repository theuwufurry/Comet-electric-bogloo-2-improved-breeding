package com.ixume.particleemitter.emitter.action.sub

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.emitter.UnrealizedEmitter
import com.ixume.particleemitter.emitter.action.ActionContext
import com.ixume.particleemitter.emitter.action.SubAction
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import org.bukkit.Location
import org.bukkit.util.Vector

class SpawnEmitterSubAction(private val unrealizedEmitterID: String, private val magnitude: Float, private val myEmitterData: EmitterData) : SubAction, PostInit {
    companion object : ComponentParser<SpawnEmitterSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpawnEmitterSubAction? {
            if (!jsonElement.isJsonObject) return null
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val unrealizedEmitterID = engine.compile(jsonObject.expression("emitter") ?: return null).eval() as String
            val offsetMagnitude = jsonObject.getAsJsonPrimitive("offset")?.asNumber?.toFloat() ?: 0f

            return SpawnEmitterSubAction(unrealizedEmitterID, offsetMagnitude, emitterData)
        }
    }

    private lateinit var unrealizedEmitter: UnrealizedEmitter

    override fun realize() {
        unrealizedEmitter = ParticleJsonParser.jsonUnrealizedEmitters[unrealizedEmitterID]!!
    }

    override fun execute(context: ActionContext) {
        if (context.pos == null || context.dir == null) {
            throw NullPointerException("Cannot use Spawn Emitter SubAction in this event!")
        }

        myEmitterData.copyFrom(context.otherEmitterData)

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
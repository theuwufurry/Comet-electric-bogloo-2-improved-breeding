package gg.aquatic.comet.v2.parsing.api

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.runtime.EffectInitializationRequest
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import gg.aquatic.comet.v2.runtime.audience.Audience
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.virtual.OptimizationSettings
import org.bukkit.World
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value

class JSEffectAPI(
    val context: Context,
) {
    @JvmField val optimization = OptimizationSettings(
        enabled = true,
        positionTolerance = 0.1,
        scaleTolerance = 0.1,
        colorTolerance = 24.0,
        rotTolerance = 0.1,
        opacityTolerance = 24.0,
        interval = 100
    )
    
    val listeners = mutableMapOf<String, MutableList<Value>>()
    val distinctCallbacks = mutableMapOf<String, Value>()

    @HostAccess.Export
    fun addListener(event: String, listener: Value) {
        if (!listener.canExecute()) {
            throw IllegalArgumentException("Listener must be a callable function")
        }
        listeners.computeIfAbsent(event) { mutableListOf() }.add(listener)
    }

    @HostAccess.Export
    fun setCallback(name: String, callback: Value) {
        if (!callback.canExecute()) {
            throw IllegalArgumentException("Listener must be a callable function")
        }

        distinctCallbacks[name] = callback
    }

    fun invokeEffectInit(effect: V2EffectProxy) {
        listeners[EFFECT_INIT_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(effect)
            } catch (e: Exception) {
                println("Error invoking listener for $EFFECT_TICK_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeEffectTick(effect: V2EffectProxy) {
        listeners[EFFECT_TICK_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(effect)
            } catch (e: Exception) {
                println("Error invoking listener for $EFFECT_TICK_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeEffectDeath(effect: V2EffectProxy) {
        listeners[EFFECT_DEATH_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(effect)
            } catch (e: Exception) {
                println("Error invoking listener for $EFFECT_DEATH_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeParticleInit(particleData: V2ParticleData) {
        listeners[PARTICLE_INIT_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(particleData)
            } catch (e: Exception) {
                println("Error invoking listener for $PARTICLE_INIT_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeParticleTick(particleData: V2ParticleData) {
        listeners[PARTICLE_TICK_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(particleData)
            } catch (e: Exception) {
                println("Error invoking listener for $PARTICLE_TICK_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeParticleDeath(particleData: V2ParticleData) {
        listeners[PARTICLE_DEATH_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(particleData)
            } catch (e: Exception) {
                println("Error invoking listener for $PARTICLE_DEATH_EVENT_ID")
                e.printStackTrace()
            }
        }
    }

    fun invokeListeners(event: String, vararg args: Any?) {
        listeners[event]?.forEach { listener ->
            try {
                listener.executeVoid(*args)
            } catch (e: Exception) {
                println("Error invoking listener for $event: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun invokeCallback(name: String, vararg args: Any?): Value? {
        return distinctCallbacks[name]?.execute(*args)
    }

    fun realize(
        world: World,
        pose: Pose,
        parent: Parent?,
        data: JsonElement,
        extraData: MutableMap<String, Any?>,
        audience: Audience,
        callback: (Effect) -> Unit = {},
    ) {
        world.cometRuntime.registerRequest(
            EffectInitializationRequest(
                unrealized = this,
                pose = pose,
                parent = parent,
                data = data,
                extraData = extraData,
                audience = audience,
                callback
            )
        )
    }

    fun close() {
        context.close()
    }

    companion object {
        const val EFFECT_INIT_EVENT_ID = "effectInit"
        const val EFFECT_TICK_EVENT_ID = "effectTick"
        const val EFFECT_DEATH_EVENT_ID = "effectDeath"

        const val PARTICLE_INIT_EVENT_ID = "particleInit"
        const val PARTICLE_TICK_EVENT_ID = "particleTick"
        const val PARTICLE_DEATH_EVENT_ID = "particleDeath"
    }
}
package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.runtime.EffectInitializationRequest
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.bukkit.World
import org.bukkit.entity.Player
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value

class JSEffectAPI(
    val context: Context,
) {
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

    @Synchronized
    fun invokeEffectInit(effect: Effect) {
        listeners[EFFECT_INIT_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(effect)
            } catch (e: Exception) {
                println("Error invoking listener for $EFFECT_TICK_EVENT_ID")
                println(e.message)
            }
        }
    }

    @Synchronized
    fun invokeEffectTick(effect: Effect) {
        listeners[EFFECT_TICK_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(effect)
            } catch (e: Exception) {
                println("Error invoking listener for $EFFECT_TICK_EVENT_ID")
                println(e.message)
            }
        }
    }

    @Synchronized
    fun invokeParticleInit(particleData: V2ParticleData) {
        listeners[PARTICLE_INIT_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(particleData)
            } catch (e: Exception) {
                println("Error invoking listener for $PARTICLE_INIT_EVENT_ID")
                println(e.message)
            }
        }
    }

    @Synchronized
    fun invokeParticleTick(particleData: V2ParticleData) {
        listeners[PARTICLE_TICK_EVENT_ID]?.forEach { listener ->
            try {
                listener.executeVoid(particleData)
            } catch (e: Exception) {
                println("Error invoking listener for $PARTICLE_TICK_EVENT_ID")
                println(e.message)
            }
        }
    }

    @Synchronized
    fun invokeShowPlayer(player: Player): Boolean? {
        return distinctCallbacks[SHOW_PLAYER_CALLBACK_ID]?.execute(player)?.asBoolean()
    }

    @Synchronized
    fun invokeListeners(event: String, vararg args: Any?) {
        listeners[event]?.forEach { listener ->
            try {
                listener.executeVoid(*args)
            } catch (e: Exception) {
                println("Error invoking listener for $event: ${e.message}")
                println(e.message)
            }
        }
    }

    @Synchronized
    fun invokeCallback(name: String, vararg args: Any?): Value? {
        return distinctCallbacks[name]?.execute(*args)
    }

    fun realize(
        world: World,
        pose: Pose,
    ) {
        world.cometRuntime.registerRequest(
            EffectInitializationRequest(
                unrealized = this,
                pose = pose,
            ) {})
    }

    companion object {
        const val EFFECT_INIT_EVENT_ID = "effectInit"
        const val EFFECT_TICK_EVENT_ID = "effectTick"

        const val PARTICLE_INIT_EVENT_ID = "particleInit"
        const val PARTICLE_TICK_EVENT_ID = "particleTick"

        const val SHOW_PLAYER_CALLBACK_ID = "showPlayer"
    }
}
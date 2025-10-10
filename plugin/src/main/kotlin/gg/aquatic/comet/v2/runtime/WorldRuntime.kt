package gg.aquatic.comet.v2.runtime

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.emitter.TemporalEffect
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

class WorldRuntime(
    val world: World,
) : EmitterRuntime {
    override val players: Collection<Player>
        get() = world.players

    private val initializationRequests = ConcurrentLinkedQueue<EffectInitializationRequest>()
    private val effectsToRemove = ConcurrentLinkedQueue<Effect>()
    private val effects = CopyOnWriteArrayList<Effect>()

    private var task: BukkitTask? = null

    init {
        runtimes[world] = this
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(AbstractParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    private fun tick() {
        processInitializations()

        for (emitter in effects) {
            emitter.tick()
        }
    }

    private fun processInitializations() {
        val added = mutableListOf<Effect>()
        var req: EffectInitializationRequest? = null
        while (initializationRequests.poll()?.let { req = it } != null) {
            req!!
            val emitter = TemporalEffect(
                pose = req.pose,
                runtime = this,
                api = req.unrealized,
            )

            added += emitter
        }

        effects += added
    }

    private fun processRemovals() {
        var effect: Effect? = null
        while (effectsToRemove.poll()?.let { effect = it } != null) {
            effect!!

            effect.onKill()
        }

        effects -= effectsToRemove
    }

    fun registerRequest(request: EffectInitializationRequest) {
        initializationRequests += request
    }

    fun clear() {
        initializationRequests.clear()
    }

    fun kill() {
        initializationRequests.clear()
        task?.cancel()
        task = null
        runtimes -= world
    }

    companion object {
        val runtimes = mutableMapOf<World, WorldRuntime>()

        val World.cometRuntime: WorldRuntime
            get() = runtimes.getOrPut(this) { WorldRuntime(this) }
    }
}
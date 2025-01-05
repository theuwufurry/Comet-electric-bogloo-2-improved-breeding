package gg.aquatic.comet.emitter

import gg.aquatic.comet.Component
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import gg.aquatic.waves.util.toUser
import io.ktor.util.collections.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

object EmitterTickersHolder {
    val unrealizedEmitters: MutableList<UnrealizedEmitter> = mutableListOf()

    fun kill() {
        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.kill()
        }

        unrealizedEmitters.clear()
    }

    fun killInstances() {
        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.killInstances()
        }
    }
}

data class UnrealizedEmitter(
    val components: List<Component>,
    val rateComponent: RateComponent,
    val shapeComponent: ShapeComponent,
    val distanceCullingComponent: DistanceCullingComponent,
    val updateFrequencyComponent: UpdateFrequencyComponent,
    val billboardConstraints: BillboardConstraints,
) {
    private val emitters: MutableSet<Emitter> = ConcurrentSet()
    private var tasks: BukkitTask
    val myEntityDataBuilder = EntityDataBuilder()

    init {
        EmitterTickersHolder.unrealizedEmitters += this
        tasks = Bukkit.getScheduler().runTaskTimerAsynchronously(ParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    fun kill() {
        killInstances()
        tasks.cancel()
    }

    fun killInstances() {
        emitters.forEach { it.kill() }
    }

    private fun tick() {
        val deadEmitters = HashSet<Emitter>()
        val playerDeadParticleMap: MutableMap<Player, MutableList<Int>> = mutableMapOf()
        for (emitter in emitters) {
            val result = emitter.tick()
            if (!result.alive) deadEmitters += emitter
            for ((player, ids) in result.deadParticles) {
                val entry = playerDeadParticleMap[player]
                if (entry == null) {
                    playerDeadParticleMap[player] = ids
                } else {
                    entry.addAll(ids)
                }
            }
        }

        for ((player, ids) in playerDeadParticleMap) {
            player.toUser().sendPacket(WrapperPlayServerDestroyEntities(*ids.toIntArray()))
        }

        emitters.removeAll(deadEmitters)
    }

    fun realize(location: Location, audience: AquaticAudience = GlobalAudience()): Emitter {
        val emitterData = EmitterData()
        emitterData.world = location.world
        emitterData.location = location
        return Emitter(
            components,
            rateComponent,
            shapeComponent,
            distanceCullingComponent,
            updateFrequencyComponent,
            billboardConstraints, location, emitterData, this, audience
        ).also {
            emitters += it
        }
    }
}
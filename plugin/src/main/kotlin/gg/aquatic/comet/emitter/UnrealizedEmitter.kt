package gg.aquatic.comet.emitter


import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.EmitterTickersHolder
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import gg.aquatic.waves.util.toUser
import io.ktor.util.collections.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d

data class UnrealizedEmitter(
    override val components: List<Component>,
    val rateComponent: RateComponent,
    val distanceCullingComponent: DistanceCullingComponent,
    val updateFrequencyComponent: UpdateFrequencyComponent,
    override val billboardConstraints: BillboardConstraints,
    override val forwardVector: Vector3d,
    val isListed: Boolean
) : AbstractUnrealizedEmitter() {
    private val emitters: MutableSet<Emitter> = ConcurrentSet()
    private var tasks: BukkitTask

    init {
        EmitterTickersHolder.unrealizedEmitters += this
        tasks = Bukkit.getScheduler().runTaskTimerAsynchronously(AbstractParticleEmitter.INSTANCE, Runnable {
            tick()
        }, 1, 1)
    }

    override fun kill() {
        killInstances()
        tasks.cancel()
    }

    override fun killInstances() {
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
            if (player.toUser() == null) continue
            player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids.toIntArray()))
        }

        emitters.removeAll(deadEmitters)
    }

    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience
    ): Emitter {
        val emitterData = EmitterData()
        emitterData.world = location.world
        emitterData.location = location
        emitterData.variable.putAll(environmentData.data)
        return Emitter(
            parent,
            components,
            rateComponent,
            distanceCullingComponent,
            updateFrequencyComponent,
            billboardConstraints, location, emitterData, this, forwardVector, environmentData, audience
        ).also {
            emitters += it
        }
    }

    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
    ): Emitter {
        return realize(parent, location, environmentData, GlobalAudience())
    }
}
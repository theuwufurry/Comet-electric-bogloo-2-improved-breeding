package gg.aquatic.comet.emitter

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.bundle.BundledEmitterComponent
import gg.aquatic.comet.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.recursive.RecursiveEmitterComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.particle.color.ColorComponent
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.transformation.rotation.RotationComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
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
    val rateComponent: RateComponent,
    val particleLifetimeComponent: ParticleLifetimeComponent,
    val displayComponent: DisplayComponent,
    val shapeComponent: ShapeComponent,
    val colorComponent: ColorComponent,
    val emitterLifetimeComponent: EmitterLifetimeComponent,
    val positionComponent: PositionComponent,
    val scaleComponent: ScaleComponent,
    val rotationComponent: RotationComponent,
    val recursiveEmitterComponent: RecursiveEmitterComponent?,
    val bundledEmitterComponent: BundledEmitterComponent?,
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
        bundledEmitterComponent?.init(emitterData)
        return Emitter(
            rateComponent,
            particleLifetimeComponent,
            shapeComponent,
            displayComponent,
            colorComponent,
            emitterLifetimeComponent,
            positionComponent,
            scaleComponent,
            rotationComponent,
            recursiveEmitterComponent,
            bundledEmitterComponent,
            billboardConstraints, location, emitterData, this, audience
        ).also { emitters += it }
    }
}
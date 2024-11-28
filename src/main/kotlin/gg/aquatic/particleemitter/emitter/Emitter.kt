package gg.aquatic.particleemitter.emitter

import gg.aquatic.aquaticseries.lib.audience.AquaticAudience
import gg.aquatic.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.particleemitter.emitter.rate.RateComponent
import gg.aquatic.particleemitter.emitter.shape.ShapeComponent
import gg.aquatic.particleemitter.particle.Particle
import gg.aquatic.particleemitter.particle.ParticleData
import gg.aquatic.particleemitter.particle.color.ColorComponent
import gg.aquatic.particleemitter.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.particleemitter.particle.position.PositionComponent
import gg.aquatic.particleemitter.particle.texture.SpriteComponent
import gg.aquatic.particleemitter.particle.transformation.scale.ScaleComponent
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.util.toUser
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

data class Emitter(
    val rateComponent: RateComponent,
    val particleLifetimeComponent: ParticleLifetimeComponent,
    val shapeComponent: ShapeComponent,
    val spriteComponent: SpriteComponent,
    val colorComponent: ColorComponent,
    val emitterLifetimeComponent: EmitterLifetimeComponent,
    val positionComponent: PositionComponent,
    val scaleComponent: ScaleComponent,
    var location: Location,
    var task: BukkitTask?,
    val audience: AquaticAudience
) {
    private val emitterData: EmitterData = EmitterData(0.0)
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false

    private var entityRemovePacket = WrapperPlayServerDestroyEntities()
    val viewers = mutableSetOf<Player>()

    fun tick() {
        if (blocked) {
            return
        }

        val packets = mutableListOf<PacketWrapper<*>>()
        blocked = true

        emitterData.age++
        if (!dead && !emitterLifetimeComponent.keepAlive(emitterData)) {
            dead = true
        }

        if (dead && particles.size == 0) {
            task!!.cancel()
            return
        }

        for (particle in particles) {
            particle.tick()
            if (!particleLifetimeComponent.keepAlive(emitterData, particle.data)) {
                deadParticles += particle
                continue
            }

            var updateParticle = false

            val newColor = colorComponent.color(emitterData, particle.data)
            if (newColor != particle.data.color) {
                updateParticle = true
                particle.data.color = newColor
            }

            val newPos = positionComponent.pos(emitterData, particle.data)
            if (newPos != particle.data.relativePosition) {
                particle.data.relativePosition = newPos
                particle.updateLocation()?.let { packets.add(it) }
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val matrix = matrixFromParts(newScale)
            if (matrix != particle.data.matrix) {
                updateParticle = true
                particle.data.matrix = matrix
            }

            if (updateParticle) {
                packets += particle.updateParticle()
            }
        }

        particles.removeAll(deadParticles)
        if (deadParticles.isNotEmpty()) {
            val removePacket = WrapperPlayServerDestroyEntities(*deadParticles.map { it.particleEntity.entityId }.toIntArray())
            packets += removePacket
            deadParticles.clear()
        }

        /*
        val ids = deadParticles.map { it.fakeEntity.entityId }.toIntArray()
        for (player in world!!.players) {
            val user = player.toUser()
            if (ids.isNotEmpty()) {
                user.sendPacket(WrapperPlayServerDestroyEntities(*ids))
            }
            for (dataPacket in dataPackets) {
                user.sendPacket(dataPacket)
            }
        }
         */


        if (!dead) packets += spawnParticles()

        for (viewer in viewers) {
            val user = viewer.toUser()
            for (packet in packets) {
                user.sendPacket(packet)
            }
        }

        blocked = false
    }

    fun addViewer(player: Player) {
        if (player in viewers) return
        val user = player.toUser()
        val packets = mutableListOf<PacketWrapper<*>>()
        for (particle in particles) {
            packets += particle.spawnPackets()
        }
        for (packet in packets) {
            user.sendPacket(packet)
        }
        viewers += player
    }

    fun removeViewer(player: Player) {
        if (player !in viewers) return
        val user = player.toUser()
        user.sendPacket(entityRemovePacket)
        viewers -= player
    }

    private fun spawnParticles(): List<PacketWrapper<*>> {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            var particleData = ParticleData()
            val spawnOffset = shapeComponent.offset(emitterData, particleData)
            val matrix = matrixFromParts(scaleComponent.scale(emitterData, particleData))
            particleData = ParticleData(
                0.0,
                spriteComponent.sprite(emitterData, particleData),
                colorComponent.color(emitterData, particleData),
                Vector3d(0.0),
                matrix,
                particleData.random
            )
            val particle = Particle(
                this,
                location.clone().apply {
                    yaw = 0f
                    pitch = 0f
                },
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z),
                particleData
            )
            //val packets = particle.getAddPacket()
            //bundle.add(packets.first)
            //bundle.add(packets.second)
            particles += particle
            bundle += particle.spawnPackets()
            entityRemovePacket.entityIds += particle.particleEntity.entityId
        }

        /*
        for (player in location.world!!.players) {
            val user = player.toUser()
            for (packetWrapper in bundle) {
                user.sendPacket(packetWrapper)
            }
        }
         */
        return bundle
    }

    private fun matrixFromParts(scale: Vector3f): Matrix4f {
        val matrix = Matrix4f()
        matrix.scale(scale)
        return matrix
    }
}
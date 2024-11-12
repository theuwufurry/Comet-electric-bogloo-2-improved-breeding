package gg.aquatic.particleemitter.emitter

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
    var task: BukkitTask?
) {
    private val emitterData: EmitterData = EmitterData(0.0)
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false

    fun tick() {
        if (blocked) {
            return
        }

        blocked = true

        emitterData.age++
        if (!dead && !emitterLifetimeComponent.keepAlive(emitterData)) {
            dead = true
        }

        if (dead && particles.size == 0) {
            task!!.cancel()
            return
        }

        val world = location.world
        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

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
                particle.updateLocation()
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val matrix = matrixFromParts(newScale)
            if (matrix != particle.data.matrix) {
                updateParticle = true
                particle.data.matrix = matrix
            }

            if (updateParticle) {
                particle.updateParticle()
            }
        }

        particles.removeAll(deadParticles)
        //val dataUpdatePacket = ClientboundBundlePacket(dataPackets)
        val ids = deadParticles.map { it.id }.toIntArray()
        for (player in world!!.players) {
            val user = player.toUser()
            if (ids.isNotEmpty()) {
                user.sendPacket(WrapperPlayServerDestroyEntities(*ids))
            }
            for (dataPacket in dataPackets) {
                user.sendPacket(dataPacket)
            }
        }

        deadParticles.clear()

        if (!dead) spawnParticles()

        blocked = false
    }

    fun spawnParticles() {
        //val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
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
        }

        /*
        for (player in location.world!!.players) {
            val user = player.toUser()
            for (packetWrapper in bundle) {
                user.sendPacket(packetWrapper)
            }
        }
         */
    }

    private fun matrixFromParts(scale: Vector3f): Matrix4f {
        val matrix = Matrix4f()
        matrix.scale(scale)
        return matrix
    }
}
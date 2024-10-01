package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.sprite.SpriteComponent
import com.ixume.particleemitter.particle.Particle
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.scheduler.BukkitTask
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

class Emitter(private val rateComponent: RateComponent,
              private val particleLifetimeComponent: ParticleLifetimeComponent,
              private val shapeComponent: ShapeComponent,
              private val spriteComponent: SpriteComponent,
              private val colorComponent: ColorComponent,
              private val emitterLifetimeComponent: EmitterLifetimeComponent,
              private val positionComponent: PositionComponent,
              private val scaleComponent: ScaleComponent,
              private var location: Location,
              var task: BukkitTask?) {
    private val emitterData: EmitterData = EmitterData()
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
        val dataPackets: MutableList<Packet<in ClientGamePacketListener>> = mutableListOf()

        for (particle in particles) {
            particle.tick()
            if (!particleLifetimeComponent.keepAlive(emitterData, particle.data)) {
                deadParticles += particle
                continue
            }

            var updateParticle = false

            val newSprite = spriteComponent.sprite(emitterData, particle.data)
            if (newSprite != particle.data.sprite) {
                updateParticle = true
                particle.data.sprite = newSprite
            }

            val newColor  = colorComponent.color(emitterData, particle.data)
            if (newColor != particle.data.color) {
                updateParticle = true
                particle.data.color = newColor
            }

            val newPos = positionComponent.pos(emitterData, particle.data)
            if (newPos != particle.data.relativePosition) {
                particle.data.relativePosition = newPos
                dataPackets += particle.getMovementPacket()
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val matrix = matrixFromParts(newScale)
            if (matrix != particle.data.matrix) {
                updateParticle = true
                particle.data.matrix = matrix
            }

            if (updateParticle) particle.updatePacket()?.let { dataPackets += it }
        }

        particles.removeAll(deadParticles)
        val dataUpdatePacket = ClientboundBundlePacket(dataPackets)
        val ids = IntArrayList(deadParticles.map {it.id})
        for (player in world.players) {
            (player as CraftPlayer).handle.connection.send(ClientboundRemoveEntitiesPacket(ids))
            player.handle.connection.send(dataUpdatePacket)
        }

        deadParticles.clear()

        if (!dead) spawnParticles()

        blocked = false
    }

    fun spawnParticles() {
        val bundle: MutableList<Packet<in ClientGamePacketListener>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            var particleData = ParticleData()
            val spawnOffset = shapeComponent.offset(emitterData, particleData)
            val matrix = matrixFromParts(scaleComponent.scale(emitterData, particleData))
            particleData = ParticleData(0.0, spriteComponent.sprite(emitterData, particleData), colorComponent.color(emitterData, particleData), Vector3d(), matrix, particleData.random, Vector3d())
            val relativePosition = positionComponent.pos(emitterData, particleData)
            val particle = Particle(Vector3d(location.x + spawnOffset.x + relativePosition.x, location.y + spawnOffset.y + relativePosition.y, location.z + spawnOffset.z + relativePosition.z), particleData)
            val packets = particle.getAddPacket()
            bundle.add(packets.first)
            packets.second?.let { it1 -> bundle.add(it1) }
            particles += particle
        }

        val bundlePacket = ClientboundBundlePacket(bundle)

        for (player in location.world.players) {
            (player as CraftPlayer).handle.connection.send(bundlePacket)
        }
    }

    private fun matrixFromParts(scale: Vector3f): Matrix4f {
        val matrix = Matrix4f()
        matrix.scale(scale)
        return matrix
    }
}
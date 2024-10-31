package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.GlobalEmitterTicker
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.recursive.RecursiveEmitterComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.Particle
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.transformation.rotation.RotationComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.world.entity.Display.BillboardConstraints
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

class Emitter(
    private val rateComponent: RateComponent,
    private val particleLifetimeComponent: ParticleLifetimeComponent,
    private val shapeComponent: ShapeComponent,
    private val displayComponent: DisplayComponent,
    private val colorComponent: ColorComponent,
    private val emitterLifetimeComponent: EmitterLifetimeComponent,
    private val positionComponent: PositionComponent,
    private val scaleComponent: ScaleComponent,
    private val rotationComponent: RotationComponent,
    private val recursiveEmitterComponent: RecursiveEmitterComponent?,
    private val billboardConstraints: BillboardConstraints,
    @Volatile private var location: Location,
    private val emitterData: EmitterData
) {
    //origin can change, rotation can change
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    var dead = false

    init {
        GlobalEmitterTicker.emitters += this
    }

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
            GlobalEmitterTicker.emitters.remove(this)
            return
        }

        val world = location.world
        val dataPackets: MutableList<Packet<in ClientGamePacketListener>> = mutableListOf()

        for (particle in particles) {
            particle.tick()
            recursiveEmitterComponent?.updateEmitter(emitterData, particle.data)

            if (!particleLifetimeComponent.keepAlive(emitterData, particle.data)) {
                deadParticles += particle
                continue
            }

            var updateParticle = false

            val newDisplay = displayComponent.display(emitterData, particle.data)
            if (newDisplay != particle.data.displayData) {
                updateParticle = true
                particle.data.displayData = newDisplay
            }

            val newColor = colorComponent.color(emitterData, particle.data)
            if (newColor != particle.data.color) {
                updateParticle = true
                particle.data.color = newColor
            }

            val newPos = positionComponent.pos(emitterData, particle.data)
            if (newPos != particle.data.relativePosition) {
                particle.data.relativePosition = newPos
                particle.getMovementPacket().let { dataPackets += it }
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val newRotation = rotationComponent.rotation(emitterData, particle.data)
            val matrix = matrixFromParts(newScale, newRotation)
            if (matrix != particle.data.matrix) {
                updateParticle = true
                particle.data.matrix = matrix
            }

            if (updateParticle) particle.updatePacket()?.let { dataPackets += it }
        }

        particles.removeAll(deadParticles)
        val dataUpdatePacket = ClientboundBundlePacket(dataPackets)
        val ids = IntArrayList(deadParticles.map { it.id })
        for (player in world.players) {
            (player as CraftPlayer).handle.connection.send(ClientboundRemoveEntitiesPacket(ids))
            player.handle.connection.send(dataUpdatePacket)
        }

        deadParticles.clear()

        if (!dead) spawnParticles()

        blocked = false
    }

    private fun spawnParticles() {
        val bundle: MutableList<Packet<in ClientGamePacketListener>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData()
            val spawnOffset = shapeComponent.offset(emitterData, particleData)
            val matrix = matrixFromParts(
                scaleComponent.scale(emitterData, particleData),
                rotationComponent.rotation(emitterData, particleData)
            )
            particleData.relativePosition = positionComponent.pos(emitterData, particleData)
            particleData.origin =
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z)
            particleLifetimeComponent.keepAlive(emitterData, particleData)
            particleData.displayData = displayComponent.display(emitterData, particleData)
            particleData.color = colorComponent.color(emitterData, particleData)
            particleData.billboardConstraints = billboardConstraints
            particleData.matrix = matrix
            recursiveEmitterComponent?.run { updateEmitter(emitterData, particleData) }
//            particleData = ParticleData(origin = particleData.origin, relativePosition = relativePosition, displayData = displayComponent.display(particleData), color = colorComponent.color(particleData), matrix = matrix, random = particleData.random, billboardConstraints = billboardConstraints)
            val particle = Particle(particleData)
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

    fun setPos(x: Double, y: Double, z: Double) {
        location.x = x
        location.y = y
        location.z = z
    }

    private fun matrixFromParts(scale: Vector3f, rotation: Matrix4f): Matrix4f {
        val matrix = Matrix4f()
        matrix.mul(rotation).scale(scale).translate(-0.0125f, 0f, 0f)
        return matrix
    }
}
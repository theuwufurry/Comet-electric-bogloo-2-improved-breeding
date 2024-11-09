package com.ixume.particleemitter.emitter

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
import org.joml.Quaternionf
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
    private var location: Location,
    private val emitterData: EmitterData,
    private val unrealizedHolder: UnrealizedEmitter
) {
    //origin can change, rotation can change
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false
    private val emitterRotation = Quaternionf().rotateTo(Vector3f(0f, 0f, 1f), location.direction.normalize().toVector3f())

    fun tick(): Boolean {
        if (blocked) {
            return true
        }

        blocked = true

        emitterData.age++

        if (!dead && !emitterLifetimeComponent.keepAlive(emitterData)) {
            dead = true
        }

        if (dead && particles.size == 0) {
            return false
        }

        val world = location.world
        val dataPackets: MutableList<Packet<in ClientGamePacketListener>> = mutableListOf()

        //loop through particles, only connection to emitter is emitterData
        //data packets can be done per thread
        //so:
        //call update on particles in object Ticker
        //each component has its own list of compiled scripts
        //number of compiled scripts per component should equal to thread count
        //different compiled scripts means different myEmitterData and myParticleData
        //ticker signals which thread it's on with simple index.
        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.emitter?.dead = true
            }

            particle.tick()
            recursiveEmitterComponent?.updateEmitter(emitterData, particle.data)

            if (!particleLifetimeComponent.keepAlive(emitterData, particle.data)) {
                die()
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

            if (!newPos.keepAlive) {
                die()
                continue
            }

            if (newPos.data != particle.data.relativePosition) {
                particle.data.relativePosition = newPos.data
                particle.getMovementPacket().let { dataPackets += it }
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val newRotation = rotationComponent.rotation(emitterData, particle.data)
            val matrix = matrixFromParts(newScale, newRotation)
            if (matrix != particle.data.matrix) {
                updateParticle = true
                particle.data.matrix = matrix
            }

            if (updateParticle) particle.updatePacket(unrealizedHolder.myEntityDataBuilder)?.let { dataPackets += it }
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
        return true
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
            particleData.relativePosition = positionComponent.pos(emitterData, particleData).data
            particleData.origin =
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z)
            particleLifetimeComponent.keepAlive(emitterData, particleData)
            particleData.displayData = displayComponent.display(emitterData, particleData)
            particleData.color = colorComponent.color(emitterData, particleData)
            particleData.billboardConstraints = billboardConstraints
            particleData.matrix = matrix
            recursiveEmitterComponent?.run { updateEmitter(emitterData, particleData) }
            val particle = Particle(particleData)
            val packets = particle.getAddPacket(unrealizedHolder.myEntityDataBuilder)
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

    fun matrixFromParts(scale: Vector3f, rotation: Matrix4f): Matrix4f {
        val matrix = Matrix4f()
        return (if (billboardConstraints == BillboardConstraints.FIXED) matrix.rotate(emitterRotation) else matrix).mul(rotation).scale(scale).translate(-0.0125f, 0f, 0f)
    }
}
package gg.aquatic.comet.emitter

//import net.minecraft.world.entity.Display.BillboardConstraints
//import net.minecraft.network.protocol.Packet
//import net.minecraft.network.protocol.game.ClientGamePacketListener
//import net.minecraft.network.protocol.game.ClientboundBundlePacket
//import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
//import net.minecraft.world.entity.Display.BillboardConstraints
import gg.aquatic.comet.emitter.bundle.BundledEmitterComponent
import gg.aquatic.comet.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.recursive.RecursiveEmitterComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.color.ColorComponent
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.transformation.rotation.RotationComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import org.bukkit.Location
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

const val MAX_VIEW_DISTANCE = 100*100
const val MAX_UNVIEW_DISTANCE = 200*200

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
    private val bundledEmitterComponent: BundledEmitterComponent?, //KEEP THIS AROUND! Might be needed for future variable stuff.
    private val billboardConstraints: BillboardConstraints,
    location: Location,
    private val emitterData: EmitterData,
    private val unrealizedHolder: UnrealizedEmitter
) {
    var location = location
        private set
    //origin can change, rotation can change
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private var blocked = false
    private var dead = false
    private val emitterRotation =
        Quaternionf().rotateTo(Vector3f(0f, 0f, 1f), location.direction.normalize().toVector3f())

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
        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

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

            if (particle.data.dead) {
                die()
                continue
            }

            if (newPos.data != particle.data.relativePosition) {
                particle.data.relativePosition = newPos.data
                particle.getMovementPacket().let { dataPackets += it }
            }

            val newScale = scaleComponent.scale(emitterData, particle.data)
            val newRotation = rotationComponent.rotation(emitterData, particle.data).applyEmitterRotation()
            if (particle.data.scale != newScale || particle.data.rotation != newRotation) {
                updateParticle = true
                particle.data.scale = newScale
                particle.data.rotation = newRotation
            }

            if (updateParticle) particle.updatePacket(unrealizedHolder.myEntityDataBuilder).let { dataPackets += it }
        }

        val playerManager = PacketEvents.getAPI().playerManager

        particles.removeAll(deadParticles)
        val ids = deadParticles.map { it.id }.toIntArray()
        for (player in world!!.players) {
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < MAX_UNVIEW_DISTANCE) playerManager.sendPacket(player, WrapperPlayServerDestroyEntities(*ids))
            if (distanceSquared < MAX_VIEW_DISTANCE)
            for (packet in dataPackets) {
                playerManager.sendPacket(player, packet)
            }
        }

        deadParticles.clear()

        if (!dead) spawnParticles()

        blocked = false
        return true
    }

    private fun spawnParticles() {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData()
            val spawnOffset = shapeComponent.offset(emitterData, particleData)
            val scale = scaleComponent.scale(emitterData, particleData)
            val rotation = rotationComponent.rotation(emitterData, particleData)
            particleData.relativePosition = positionComponent.pos(emitterData, particleData).data
            particleData.origin =
                Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z)
            particleLifetimeComponent.keepAlive(emitterData, particleData)
            particleData.displayData = displayComponent.display(emitterData, particleData)
            particleData.color = colorComponent.color(emitterData, particleData)
            particleData.billboardConstraints = billboardConstraints
            particleData.scale = scale
            particleData.rotation = rotation.applyEmitterRotation()
            recursiveEmitterComponent?.run { updateEmitter(emitterData, particleData) }
            val particle = Particle(particleData)
            val packets = particle.getAddPacket(unrealizedHolder.myEntityDataBuilder)
            bundle.addAll(packets)

            particles += particle
        }

        val playerManager = PacketEvents.getAPI().playerManager

        for (player in location.world!!.players) {
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < MAX_VIEW_DISTANCE) {
                for (packet in bundle) {
                    playerManager.sendPacket(player, packet)
                }
            }
        }
    }

    fun setPos(x: Double, y: Double, z: Double) {
        location.x = x
        location.y = y
        location.z = z
    }

    private fun Quaternionf.applyEmitterRotation(): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(this) else this
    }
}
package gg.aquatic.comet.api.particle

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ParticleData(
    var id: UUID = UUID.randomUUID(),
    var particle: AbstractParticle? = null,
    var dead: Boolean = false,
    var age: Double = 0.0,
    var maxLife: Int = 0,
    var displayData: DisplayData = SpriteData(""),
    var color: Int = 0,
    var origin: Vector3d = Vector3d(),
    var relativePosition: Vector3d = Vector3d(),
    var translation: Vector3f = Vector3f(),
    var rotation: Quaternionf = Quaternionf(),
    var scale: Vector3f = Vector3f(),
    var velocity: Vector3d = Vector3d(),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    var emitter: AbstractEmitter? = null,
    var acceleration: Vector3d = Vector3d(),
    var interpolationDelay: Int = -1,
    var interpolationDuration: Int = 2
) {
    val pos: Vector3d
        get() = Vector3d(origin).add(relativePosition)

    fun copyFrom(other: ParticleData) {
        id = other.id
        particle = other.particle
        dead = other.dead
        age = other.age
        maxLife = other.maxLife
        displayData = other.displayData
        color = other.color
        origin = other.origin
        relativePosition = other.relativePosition
        translation = other.translation
        rotation = other.rotation
        scale = other.scale
        velocity = other.velocity
        billboardConstraints = other.billboardConstraints
        acceleration = other.acceleration
        interpolationDelay = other.interpolationDelay
        interpolationDuration = other.interpolationDuration

        variable.clear()
        variable.putAll(other.variable)
    }

    val variable: MutableMap<String, Any> = ConcurrentHashMap()
}

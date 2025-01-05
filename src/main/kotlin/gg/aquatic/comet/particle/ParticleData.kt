package gg.aquatic.comet.particle

import gg.aquatic.comet.emitter.Emitter
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.display.DisplayData
import gg.aquatic.comet.particle.display.sprite.SpriteData
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*

data class ParticleData(
    var id: UUID = UUID.randomUUID(),
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
    var random: Double = Math.random(),
    var random2: Double = Math.random(),
    var random3: Double = Math.random(),
    var random4: Double = Math.random(),
    var random5: Double = Math.random(),
    var random6: Double = Math.random(),
    var random7: Double = Math.random(),
    var random8: Double = Math.random(),
    var random9: Double = Math.random(),
    var velocity: Vector3d = Vector3d(),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    var emitter: Emitter? = null,
    var acceleration: Vector3d = Vector3d(),
    var interpolationDelay: Int = -1,
    var interpolationDuration: Int = 2
) {

    fun copyFrom(other: ParticleData) {
        id = other.id
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
        random = other.random
        random2 = other.random2
        random3 = other.random3
        random4 = other.random4
        random5 = other.random5
        random6 = other.random6
        random7 = other.random7
        random8 = other.random8
        random9 = other.random9
        velocity = other.velocity
        billboardConstraints = other.billboardConstraints
        acceleration = other.acceleration
        interpolationDelay = other.interpolationDelay
        interpolationDuration = other.interpolationDuration
    }
}

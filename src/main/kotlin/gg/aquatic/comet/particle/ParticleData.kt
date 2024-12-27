package gg.aquatic.comet.particle

import gg.aquatic.comet.emitter.Emitter
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.display.DisplayData
import gg.aquatic.comet.particle.display.sprite.SpriteData
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

data class ParticleData(
    var dead: Boolean = false,
    var age: Double = 0.0,
    var maxLife: Int = 0,
    var displayData: DisplayData = SpriteData(""),
    var color: Int = 0,
    var origin: Vector3d = Vector3d(),
    var relativePosition: Vector3d = Vector3d(),
    var oldRelativePosition: Vector3d = Vector3d(),
    var translation: Vector3f = Vector3f(),
    var rotation: Quaternionf = Quaternionf(),
    var scale: Vector3f = Vector3f(),
    var random: Double = Math.random(),
    var velocity: Vector3d = Vector3d(),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    var emitter: Emitter? = null,
    var acceleration: Vector3d = Vector3d()
) {

    fun copyFrom(other: ParticleData) {
        dead = other.dead
        age = other.age
        maxLife = other.maxLife
        displayData = other.displayData
        color = other.color
        origin = other.origin
        relativePosition = other.relativePosition
        oldRelativePosition = other.oldRelativePosition
//        matrix = other.matrix
        translation = other.translation
        rotation = other.rotation
        scale = other.scale
        random = other.random
        velocity = other.velocity
        billboardConstraints = other.billboardConstraints
        acceleration = other.acceleration
    }
}

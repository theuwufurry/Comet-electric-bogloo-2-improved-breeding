package gg.aquatic.particleemitter.particle

import org.joml.Matrix4f
import org.joml.Vector3d

data class ParticleData(var age: Double,
                        var sprite: String,
                        var color: Int,
                        var relativePosition: Vector3d,
                        var matrix: Matrix4f,
                        var random: Double) {
    constructor() : this(0.0, "", 0, Vector3d(0.0), Matrix4f(), Math.random())

    fun copyFrom(other: ParticleData) {
        age = other.age
        sprite = other.sprite
        color = other.color
        relativePosition = other.relativePosition
        matrix = other.matrix
        random = other.random
    }
}

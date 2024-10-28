package com.ixume.particleemitter.particle

import com.ixume.particleemitter.particle.display.DisplayData
import com.ixume.particleemitter.particle.display.sprite.SpriteData
import net.minecraft.world.entity.Display.BillboardConstraints
import org.joml.Matrix4f
import org.joml.Vector3d

data class ParticleData(
    var age: Double = 0.0,
    var displayData: DisplayData = SpriteData(""),
    var color: Int = 0,
    var origin: Vector3d = Vector3d(),
    var relativePosition: Vector3d = Vector3d(),
    var oldRelativePosition: Vector3d = Vector3d(),
    var matrix: Matrix4f = Matrix4f(),
    var random: Double = Math.random(),
    var velocity: Vector3d = Vector3d(),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER
) {

    fun copyFrom(other: ParticleData) {
        age = other.age
        displayData = other.displayData
        color = other.color
        origin = other.origin
        relativePosition = other.relativePosition
        oldRelativePosition = other.oldRelativePosition
        matrix = other.matrix
        random = other.random
        velocity = other.velocity
        billboardConstraints = other.billboardConstraints
    }
}

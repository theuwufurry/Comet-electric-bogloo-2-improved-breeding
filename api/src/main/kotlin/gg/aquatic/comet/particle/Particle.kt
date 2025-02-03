package gg.aquatic.comet.particle

import gg.aquatic.comet.emitter.parent.Parent
import gg.aquatic.comet.emitter.parent.Pose

open class Particle(var data: ParticleData) : Parent {
    override fun pose(): Pose {
        return Pose(
            org.joml.Vector3d(
                data.origin.x + data.relativePosition.x,
                data.origin.y + data.relativePosition.y,
                data.origin.z + data.relativePosition.z
            ),
            org.joml.Vector3d()
        )
    }
}
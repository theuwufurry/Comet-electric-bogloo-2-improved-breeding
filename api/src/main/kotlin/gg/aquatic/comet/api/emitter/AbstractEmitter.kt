package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.particle.AbstractParticle
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

abstract class AbstractEmitter : Parent {
    abstract val particles: List<AbstractParticle>

    abstract val location: Location
    abstract val forwardVector: Vector3d
    abstract val environmentData: EnvironmentData

    abstract var emitterRotation: Quaterniond

    abstract fun calculateEmitterRotation(): Quaterniond

    abstract fun tick(): EmitterTickResult

    abstract val players: List<Player>

    abstract fun setPose(pose: Pose)

    abstract fun kill()

    abstract fun applyEmitterRotation(input: Quaternionf): Quaternionf

    override var dead: Boolean = false
}

class EmitterTickResult(val alive: Boolean, val deadParticles: List<Pair<Player, MutableList<Int>>> = listOf())

fun Vector3d.toVector3f(): Vector3f {
    return Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
}
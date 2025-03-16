package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.Location
import org.bukkit.entity.Player
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.UUID

abstract class AbstractEmitter : Parent {
    abstract val id: UUID
    abstract val particles: List<AbstractParticle>

    abstract val location: Location
    abstract val forwardVector: Vector3d
    abstract val environmentData: EnvironmentData

    abstract var emitterRotation: Quaterniond

    abstract fun tick(): EmitterTickResult

    abstract val players: List<Player>

    abstract fun setPose(pose: Pose)

    abstract fun kill()

    abstract fun applyEmitterRotation(input: Quaternionf): Quaternionf

    override var dead: Boolean = false

    abstract val audience: AquaticAudience

    abstract val random: DeterministicRandom

    abstract val isPregen: Boolean

    abstract fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience,
        random: DeterministicRandom,
    )
}

class EmitterTickResult(val alive: Boolean, val deadParticles: List<Pair<Player, MutableList<Int>>> = listOf())
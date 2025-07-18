package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.Mount
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.entity.Player
import org.joml.Quaternionf
import java.util.*
import java.util.function.Supplier

abstract class AbstractEmitter : Parent {
    abstract val id: UUID
    abstract val particles: MutableList<out AbstractParticle>

    abstract val unrealizedEmitter: AbstractUnrealizedEmitter

    abstract val environmentData: EnvironmentData

    abstract fun tick(): EmitterTickResult

    abstract val players: List<Player>

    abstract override var pose: Pose

    abstract fun kill()

    abstract fun applyEmitterRotation(input: Quaternionf): Quaternionf

    override var dead: Boolean = false

    abstract val audience: AquaticAudience

    abstract val random: DeterministicRandom

    abstract val isPregen: Boolean

    abstract val mount: Mount?

    abstract val yawpitchSupplier: Supplier<YawPitch>?

    abstract fun getSpawnPackets(): List<Any>

    abstract fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent? = null,
        pose: Pose,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID = random.uuid()
    )
}

class EmitterTickResult(val alive: Boolean, val deadParticles: List<Pair<Player, MutableList<Int>>> = listOf())
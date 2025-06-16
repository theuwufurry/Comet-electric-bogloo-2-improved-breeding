package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.Mount
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector2f
import org.joml.Vector3d
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

abstract class AbstractUnrealizedEmitter {
    abstract val id: String

    abstract val preInitComponents: List<PreInitComponent>
    abstract val components: List<Component>
    abstract val billboardConstraints: BillboardConstraints
    abstract val forwardVector: Vector3d

    abstract val isDoubleSided: Boolean
    abstract val lookahead: Int

    /**
     * Whether the particle is spawned for incoming players
     */
    abstract val persistent: Boolean

    abstract fun realize(
        parent: Parent? = null,
        pose: Pose,
        environmentData: EnvironmentData = EnvironmentData(),
        mount: Mount? = null,
        yawpitchSupplier: Supplier<YawPitch>? = null,
        after: Consumer<AbstractEmitter> = Consumer<AbstractEmitter> { },
    )

    abstract fun realize(
        parent: Parent? = null,
        pose: Pose,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience = GlobalAudience(),
        mount: Mount? = null,
        yawpitchSupplier: Supplier<YawPitch>? = null,
        after: Consumer<AbstractEmitter> = Consumer<AbstractEmitter> { },
    )

    abstract fun internalRealize(
        parent: Parent? = null,
        pose: Pose,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID,
        mount: Mount?,
        yawpitchSupplier: Supplier<YawPitch>?
    ): AbstractEmitter
}

typealias YawPitch = Vector2f

val YawPitch.yaw: Float
    get() = this.x

val YawPitch.pitch: Float
    get() = this.y

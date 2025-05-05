package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector3d
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

abstract class AbstractUnrealizedEmitter {
    abstract val id: String

    abstract val preInitComponents: List<PreInitComponent>
    abstract val components: List<Component>
    abstract val billboardConstraints: BillboardConstraints
    abstract val forwardVector: Vector3d

    abstract fun realize(
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        after: Consumer<AbstractEmitter>,
    )

    abstract fun realize(
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience = GlobalAudience(),
        after: Consumer<AbstractEmitter>,
    )

    abstract fun internalRealize(
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID,
    ): AbstractEmitter
}
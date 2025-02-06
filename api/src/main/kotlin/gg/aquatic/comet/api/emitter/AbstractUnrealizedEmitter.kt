package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector3d

object EmitterTickersHolder {
    val unrealizedEmitters: MutableList<AbstractUnrealizedEmitter> = mutableListOf()

    fun kill() {
        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.kill()
        }

        unrealizedEmitters.clear()
    }

    fun killInstances() {
        for (unrealizedEmitter in unrealizedEmitters) {
            unrealizedEmitter.killInstances()
        }
    }
}

abstract class AbstractUnrealizedEmitter {
    abstract val components: List<Component>
    abstract val billboardConstraints: BillboardConstraints
    abstract val forwardVector: Vector3d

    abstract fun kill()

    abstract fun killInstances()

    abstract fun realize(
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience = GlobalAudience()
    ): AbstractEmitter
}
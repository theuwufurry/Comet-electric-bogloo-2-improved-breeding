package gg.aquatic.comet.v2.runtime

import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.entity.Player

/**
 * Provides an abstraction over the environment an emitter can access.
 */
interface EmitterRuntime {
    val players: Collection<Player>

    fun submitExecutable(boundExecutable: BoundExecutable)
    fun remove(effect: Effect)
}
package gg.aquatic.comet.v2.runtime

import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.entity.Player

/**
 * Provides an abstraction over the environment an emitter can access.
 */
interface EmitterRuntime {
    val players: Collection<Player>

//    fun submitExecutable(executable: Value)
//    fun submitExecutable(executable: Consumer<World>)
    fun submitExecutable(boundExecutable: BoundExecutable)
}
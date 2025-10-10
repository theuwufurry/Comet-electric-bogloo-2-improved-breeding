package gg.aquatic.comet.v2.runtime

import org.bukkit.World
import org.bukkit.entity.Player
import org.graalvm.polyglot.Value
import java.util.function.Consumer

/**
 * Provides an abstraction over the environment an emitter can access.
 */
interface EmitterRuntime {
    val players: Collection<Player>

    fun submitExecutable(executable: Value)
    fun submitExecutable(executable: Consumer<World>)
}
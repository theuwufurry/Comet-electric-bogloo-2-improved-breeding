package gg.aquatic.comet.v2.runtime

import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.entity.Player

/**
 * Provides an abstraction over the environment an emitter can access.
 */
interface EmitterRuntime {
    val players: Collection<Player>
}
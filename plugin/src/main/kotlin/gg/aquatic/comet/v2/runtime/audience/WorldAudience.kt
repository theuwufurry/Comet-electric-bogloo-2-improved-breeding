package gg.aquatic.comet.v2.runtime.audience

import org.bukkit.World
import org.bukkit.entity.Player

class WorldAudience(val world: World) : Audience {
    override val players: Collection<Player>
        get() = world.players

    override fun includes(player: Player): Boolean {
        return true
    }
}
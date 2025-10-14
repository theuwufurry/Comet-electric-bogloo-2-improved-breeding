package gg.aquatic.comet.v2.runtime.audience

import org.bukkit.entity.Player

class SingletonAudience(val player: Player) : Audience {
    override val players: Collection<Player> = listOf(player)

    override fun includes(player: Player): Boolean {
        return this.player == player
    }
}
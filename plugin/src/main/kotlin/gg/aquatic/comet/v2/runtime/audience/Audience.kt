package gg.aquatic.comet.v2.runtime.audience

import org.bukkit.entity.Player

interface Audience {
    val players: Collection<Player>
  
    fun includes(player: Player): Boolean
}
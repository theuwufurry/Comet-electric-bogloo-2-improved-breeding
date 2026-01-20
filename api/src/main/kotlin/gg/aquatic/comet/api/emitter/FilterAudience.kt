package gg.aquatic.comet.api.emitter

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class FilterAudience(
    private val filter: (Player) -> Boolean
) : Audience {

    // Return all UUIDs of players that pass the filter
    override val uuids: Collection<UUID>
        get() = Bukkit.getOnlinePlayers()
            .filter(filter)
            .map { it.uniqueId }

    // Check if a specific player passes the filter
    override fun canBeApplied(player: Player): Boolean {
        return filter(player)
    }
}
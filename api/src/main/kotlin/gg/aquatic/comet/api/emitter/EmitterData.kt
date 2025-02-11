package gg.aquatic.comet.api.emitter

import org.bukkit.Location
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class EmitterData(
    var id: UUID = UUID.randomUUID(),
    var dead: Boolean = false,
    var emitter: AbstractEmitter? = null,
    var age: Double = 0.0,
    var world: World? = null,
    var location: Location = Location(null, 0.0, 0.0, 0.0),
    var isActive: Boolean = true
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        dead = other.dead
        emitter = other.emitter
        age = other.age
        world = other.world
        location = other.location
        isActive = other.isActive

        variable.clear()
        variable.putAll(other.variable)
    }

    val variable: MutableMap<String, Any> = ConcurrentHashMap()
}
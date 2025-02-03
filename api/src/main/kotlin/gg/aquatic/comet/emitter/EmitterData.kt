package gg.aquatic.comet.emitter

import org.bukkit.Location
import org.bukkit.World
import java.util.*

data class EmitterData(
    var id: UUID = UUID.randomUUID(),
    var dead: Boolean = false,
    var emitter: Emitter? = null,
    var age: Double = 0.0,
    var world: World? = null,
    var location: Location = Location(null, 0.0, 0.0, 0.0)
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        dead = other.dead
        emitter = other.emitter
        age = other.age
        world = other.world
        location = other.location

        variable.clear()
        variable.putAll(other.variable)
    }

    val variable: MutableMap<String, Any> = mutableMapOf()
}
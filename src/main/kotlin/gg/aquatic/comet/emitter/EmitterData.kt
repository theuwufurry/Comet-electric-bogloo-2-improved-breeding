package gg.aquatic.comet.emitter

import org.bukkit.Location
import org.bukkit.World
import org.joml.Quaterniond
import java.util.*

data class EmitterData(
    var id: UUID = UUID.randomUUID(),
    var dead: Boolean = false,
    var emitter: Emitter? = null,
    var age: Double = 0.0,
    var world: World? = null,
    var rotation: Quaterniond? = Quaterniond(),
    var random: Double = Math.random(),
    var random2: Double = Math.random(),
    var random3: Double = Math.random(),
    var random4: Double = Math.random(),
    var location: Location = Location(null, 0.0, 0.0, 0.0)
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        dead = other.dead
        emitter = other.emitter
        age = other.age
        world = other.world
        rotation = other.rotation
        random = other.random
        random2 = other.random2
        random3 = other.random3
        random4 = other.random4
        location = other.location
    }
}
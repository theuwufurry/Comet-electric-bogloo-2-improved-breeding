package gg.aquatic.comet.emitter

import org.bukkit.Location
import org.bukkit.World
import org.joml.Quaterniond
import java.util.*

data class EmitterData(
    var id: UUID = UUID.randomUUID(),
    var age: Double = 0.0,
    var world: World? = null,
    var rotation: Quaterniond? = Quaterniond(),
    var random: Double = Math.random(),
    var location: Location = Location(null, 0.0, 0.0, 0.0)
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        age = other.age
        world = other.world
        rotation = other.rotation
        random = other.random
        location = other.location
    }
}
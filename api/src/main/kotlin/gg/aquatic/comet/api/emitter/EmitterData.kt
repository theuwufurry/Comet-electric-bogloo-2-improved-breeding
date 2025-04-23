package gg.aquatic.comet.api.emitter

import org.bukkit.Location
import org.bukkit.World
import java.util.*

data class EmitterData(
    var id: UUID = UUID.randomUUID(),
    var dead: Boolean = false,
    var emitter: AbstractEmitter? = null,
    var age: Double = 0.0,
    var world: World? = null,
    var location: Location = Location(null, 0.0, 0.0, 0.0),
    var isActive: Boolean = true,
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        dead = other.dead
        emitter = other.emitter
        age = other.age
        world = other.world
        location = other.location
        isActive = other.isActive

        variable = other.variable
//        variable.clear()
//        variable.putAll(other.variable)
    }

    var variable: MutableMap<String, Any> = VariableMutableMap()
    val externalVariable: MutableMap<String, Any> = MapWrapper(::variable)

    fun clone(): EmitterData {
        return EmitterData(id, dead, emitter, age, world, location.clone(), isActive).apply i@{
            this@i.variable = (this@EmitterData.variable as VariableMutableMap).clone()
        }
    }

    class MapWrapper(private val backer: () -> MutableMap<String, Any>) : MutableMap<String, Any> {
        override val size: Int
            get() = backer().size

        override fun containsKey(key: String): Boolean = backer().containsKey(key)

        override fun containsValue(value: Any): Boolean = backer().containsValue(value)

        override fun get(key: String): Any? = backer()[key] // or mapGetter().get(key)

        override fun isEmpty(): Boolean = backer().isEmpty()

        // Entries, Keys, Values return the views/collections from the *current* map
        override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
            get() = backer().entries

        override val keys: MutableSet<String>
            get() = backer().keys

        override val values: MutableCollection<Any>
            get() = backer().values

        override fun clear() = backer().clear()

        override fun put(key: String, value: Any): Any? = backer().put(key, value)

        override fun putAll(from: Map<out String, Any>) = backer().putAll(from)

        override fun remove(key: String): Any? = backer().remove(key)

        // You might also want to delegate equals, hashCode, and toString
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            // Delegate equals comparison to the map currently being pointed to
            return backer() == other
        }

        override fun hashCode(): Int {
            // Delegate hashCode to the map currently being pointed to
            return backer().hashCode()
        }

        override fun toString(): String {
            // Delegate toString to the map currently being pointed to
            return backer().toString()
        }
    }
}


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
    var optimizationInterval: Int = 100,
) {
    fun copyFrom(other: EmitterData) {
        id = other.id
        dead = other.dead
        emitter = other.emitter
        age = other.age
        world = other.world
        location = other.location
        isActive = other.isActive
        optimizationInterval = other.optimizationInterval

        variable = other.variable
//        variable.clear()
//        variable.putAll(other.variable)
    }

    var variable: MutableMap<String, Any> = VariableMutableMap()
    val externalVariable: MutableMap<String, Any> = MapWrapper(::variable)

    fun clone(): EmitterData {
        return EmitterData(id, dead, emitter, age, world, location.clone(), isActive, optimizationInterval).apply i@{
            this@i.variable = (this@EmitterData.variable as VariableMutableMap).clone()
        }
    }

    class MapWrapper(private val backer: () -> MutableMap<String, Any>) : MutableMap<String, Any> {
        override val size: Int
            get() = backer().size

        override fun containsKey(key: String): Boolean = backer().containsKey(key)

        override fun containsValue(value: Any): Boolean = backer().containsValue(value)

        override fun get(key: String): Any? = backer()[key]

        override fun isEmpty(): Boolean = backer().isEmpty()

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

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            return backer() == other
        }

        override fun hashCode(): Int {
            return backer().hashCode()
        }

        override fun toString(): String {
            return backer().toString()
        }
    }
}


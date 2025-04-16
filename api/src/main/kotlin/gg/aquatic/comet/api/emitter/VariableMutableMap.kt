package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.emitter.environment.Datum
import gg.aquatic.comet.api.emitter.environment.DatumColor
import gg.aquatic.comet.api.emitter.environment.DatumNum
import gg.aquatic.comet.api.emitter.environment.DatumStr
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

class VariableMutableMap(
    private val backingMap: MutableMap<String, Datum<*, *>> = ConcurrentHashMap()
) : MutableMap<String, Any> {
    fun clone(): VariableMutableMap {
        return VariableMutableMap(backingMap.mapValues { it.value.clone() }.toMutableMap())
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any>>
        get() = EntrySetView(this)
//        get() = backingMap.map { (k, v) -> VariableEntry(k, v, this) }.toMutableSet()
    override val keys: MutableSet<String>
        get() = backingMap.keys
    override val size: Int
        get() = backingMap.size
    override val values: MutableCollection<Any>
        get() = backingMap.values.map { it.value!! }.toMutableSet()

    override fun clear() {
        backingMap.clear()
    }

    override fun isEmpty(): Boolean {
        return backingMap.isEmpty()
    }

    override fun remove(key: String): Any? {
        return backingMap.remove(key)?.value
    }

    override fun putAll(from: Map<out String, Any>) {
        for ((k, v) in from) {
            if (v is Datum<*, *>) {
                backingMap[k] = v
            } else {
                v.tryAsDatum()?.let {
                    backingMap.put(k, it)
                }
            }
        }
    }

    override fun put(key: String, value: Any): Any? {
        if (value is Datum<*, *>) {
            return backingMap.put(key, value)
        }

        value.tryAsDatum()?.let {
            return backingMap.put(key, it)
        }

        return null
    }

    override fun get(key: String): Any? {
        return backingMap[key]?.value
    }

    override fun containsValue(value: Any): Boolean {
        val r = value.tryAsDatum()?.value ?: return false
        return backingMap.containsValue(r)
    }

    override fun containsKey(key: String): Boolean {
        return backingMap.containsKey(key)
    }

    class VariableEntry(
        override val key: String,
        override val value: Datum<*, *>,
        val map: VariableMutableMap,
    ) : MutableMap.MutableEntry<String, Any> {
        override fun setValue(newValue: Any): Any {
            map[key] = newValue.tryAsDatum() ?: throw InvalidDatumTypeException(newValue)
            return newValue
        }
    }

    private class EntrySetView(
        private val mapInstance: VariableMutableMap
    ) : AbstractMutableSet<MutableMap.MutableEntry<String, Any>>() {

        override val size: Int
            get() = mapInstance.backingMap.size

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Any>> {
            val backingIterator = mapInstance.backingMap.entries.iterator()

            return object : MutableIterator<MutableMap.MutableEntry<String, Any>> {
                private var currentBackingEntry: MutableMap.MutableEntry<String, Datum<*, *>>? = null

                override fun hasNext(): Boolean = backingIterator.hasNext()

                override fun next(): MutableMap.MutableEntry<String, Any> {
                    val backingEntry = backingIterator.next()
                    currentBackingEntry = backingEntry
                    return VariableEntry(backingEntry.key, backingEntry.value, mapInstance)
                }

                override fun remove() {
                    check(currentBackingEntry != null) { "next() must be called before remove()" }
                    backingIterator.remove()
                    currentBackingEntry = null
                }
            }
        }

        override fun add(element: MutableMap.MutableEntry<String, Any>): Boolean {
            throw UnsupportedOperationException("Adding entries directly to the entry set is not supported.")
        }

        override fun clear() {
            mapInstance.backingMap.clear()
        }

        override fun contains(element: MutableMap.MutableEntry<String, Any>): Boolean {
            val backingDatum = mapInstance.backingMap[element.key]
            return backingDatum != null && backingDatum.value == element.value
        }

        override fun remove(element: MutableMap.MutableEntry<String, Any>): Boolean {
            val backingDatum = mapInstance.backingMap[element.key]
            if (backingDatum != null && backingDatum.value == element.value) {
                return mapInstance.backingMap.remove(element.key) != null
            }
            return false
        }
    }

    companion object {
        fun Any.tryAsDatum(): Datum<*, *>? {
            if (this is Number) return DatumNum(this)
            if (this is String) return DatumStr(this)
            if (this is Color) return DatumColor(this)
            return null
        }
    }

    class InvalidDatumTypeException(obj: Any?) : Exception("Invalid $obj")
}
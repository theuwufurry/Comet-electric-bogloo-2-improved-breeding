package gg.aquatic.comet.emitter.optimization.Mengshe

import kotlin.math.max

class PosArrayList {
    var elements = DoubleArray(0)
    var size = 0
    
    fun from(data: List<TimestampedPos>) {
        grow(data.size * 3)
        size = data.size
        var i = 0
        while (i < data.size) {
            val p = data[i]
            elements[i * 3] = p.x
            elements[i * 3 + 1] = p.y
            elements[i * 3 + 2] = p.z
            
            i++
        }
    }

    fun x(idx: Int): Double {
        return elements[idx * 3]
    }

    fun y(idx: Int): Double {
        return elements[idx * 3 + 1]
    }

    fun z(idx: Int): Double {
        return elements[idx * 3 + 2]
    }
    
    fun grow(required: Int) {
        if (elements.size >= required) return
        elements = elements.copyOf(max(elements.size * 2, required))
    }
}
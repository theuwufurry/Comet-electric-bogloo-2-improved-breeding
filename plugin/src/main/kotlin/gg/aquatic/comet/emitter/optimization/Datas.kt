package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVector
import gg.aquatic.comet.emitter.optimization.vec.Vec
import gg.aquatic.comet.emitter.optimization.vec.WrappedPos
import java.util.*

abstract class TimestampedData(
    val time: Int,
) {
    abstract val vec: Vec
}

class TimestampedPos(
    time: Int,
    override val vec: WrappedPos
) : TimestampedData(time) {
    override fun toString(): String {
        return """
            | Time: $time
            | Pos: ${vec.vec.x} ${vec.vec.y} ${vec.vec.z}
        """.trimIndent()
    }
}

class TimestampedColoredTexture(
    val time: Int,
    private val r: Int,
    private val g: Int,
    private val b: Int,
    val displayData: DisplayData,
) {
    val color = (r shl 16) or (g shl 8) or b
    fun distanceSquared(other: TimestampedColoredTexture): Int {
        val dr = r - other.r
        val dg = g - other.g
        val db = b - other.b
        return dr * dr + dg * dg + db * db
    }

    override fun toString(): String {
        return """
            | Time: $time
            | Color: $r $g $b
            | DisplayData: $displayData
        """.trimIndent()
    }
}

class TimestampedTransformableData(
    time: Int,
    override val vec: DisplayDataVector
) : TimestampedData(time) {
    override fun toString(): String {
        return """
            t: $time
            v: $vec
        """.trimIndent()
    }
}

class TimestampedEmitterData(
    val time: Int,
    val dead: Boolean,
    val spawns: List<UUID>,
)

class TimestampedParticleActions(
    val time: Int,
    val actions: MutableList<(AbstractEmitter, AbstractParticle) -> Unit>,
)

class TimestampedEmitterActions(
    val time: Int,
    val actions: MutableList<(AbstractEmitter) -> Unit>,
)

package gg.aquatic.comet.v2.runtime.virtual

import com.ixume.optimization.TimestampedContentData
import com.ixume.optimization.TimestampedDisplayData
import com.ixume.optimization.TimestampedPos
import com.ixume.optimization.math.Quaternion
import gg.aquatic.comet.api.particle.display.DisplayData
import java.awt.Color

@JvmRecord
data class TimestampedPosition(
    val time: Int,
    val x: Double,
    val y: Double,
    val z: Double,
) {
    fun mengshe(): TimestampedPos {
        return TimestampedPos(time, x, y, z)
    }
}

@JvmRecord
data class TimestampedTransformable(
    val time: Int,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    val rotX: Float,
    val rotY: Float,
    val rotZ: Float,
    val rotW: Float,
    val opacity: Int,
) {
    fun mengshe(): TimestampedDisplayData {
        return TimestampedDisplayData(
            t = time,
            scaleX = scaleX.toDouble(),
            scaleY = scaleY.toDouble(),
            scaleZ = scaleZ.toDouble(),
            rot = Quaternion(
                rotX.toDouble(),
                rotY.toDouble(),
                rotZ.toDouble(),
                rotW.toDouble(),
            ),
            opacity = opacity,
        )
    }
}

@JvmRecord
data class TimestampedContent(
    val time: Int,
    val data: DisplayData<*>,
    val color: Int,
) {
    fun mengshe(): TimestampedContentData {
        return TimestampedContentData(
            t = time,
            content = data,
            color = Color(color),
        )
    }
}
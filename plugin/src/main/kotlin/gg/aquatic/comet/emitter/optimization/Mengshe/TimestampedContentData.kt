package gg.aquatic.comet.emitter.optimization.Mengshe

import java.awt.Color
import kotlin.math.pow
import kotlin.math.sqrt

data class TimestampedContentData(
    val t: Int,
    val content: Any,
    val color: Color,
)

data class CIELab(val L: Double, val a: Double, val b: Double)

fun Color.toCIELab(): CIELab {
    fun gammaExpand(c8: Int): Double {
        return if (c8 <= 10) {
            c8 / 3294.6
        } else {
            ((c8 + 14.025) / 269.025).pow(2.4)
        }
    }

    val r = gammaExpand(this.red)
    val g = gammaExpand(this.green)
    val b = gammaExpand(this.blue)

    val x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375
    val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
    val z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041

    val xn = x / 0.9504492182750991
    val yn = y / 1.0
    val zn = z / 1.0889166484304715

    fun labF(t: Double): Double {
        return if (t > (6.0 / 29.0).pow(3.0)) {
            t.pow(1.0 / 3.0)
        } else {
            (1.0 / 3.0) * (29.0 / 6.0).pow(2.0) * t + 4.0 / 29.0
        }
    }

    val fx = labF(xn)
    val fy = labF(yn)
    val fz = labF(zn)

    val L_ = 116.0 * fy - 16.0
    val a_ = 500.0 * (fx - fy)
    val b_ = 200.0 * (fy - fz)

    return CIELab(L_, a_, b_)
}

fun Color.scaleDistance(other: Color): Double {
    val lab1 = this.toCIELab()
    val lab2 = other.toCIELab()

    val deltaL = lab1.L - lab2.L
    val deltaA = lab1.a - lab2.a
    val deltaB = lab1.b - lab2.b

    val dE = sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB)

    return dE
}
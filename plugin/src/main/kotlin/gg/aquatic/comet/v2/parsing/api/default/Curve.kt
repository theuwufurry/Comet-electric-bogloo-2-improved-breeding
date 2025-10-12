package gg.aquatic.comet.v2.parsing.api.default

import gg.aquatic.comet.particle.macro.CatmullEvaluator

interface Curve {
    fun at(time: Double): Double
}

class CatmullCurve(val catmull: CatmullEvaluator) : Curve {
    override fun at(time: Double): Double {
        return catmull.eval(time, 0.0, 0.0)
    }
}

class LinearCurve(val xs: DoubleArray, val ys: DoubleArray) : Curve {
    override fun at(time: Double): Double {
        if (time <= xs[0]) return ys[0]
        if (time >= xs[xs.size - 1]) return ys[ys.size - 1]

        for (i in 0 until xs.size - 1) {
            if (time >= xs[i] && time <= xs[i + 1]) {
                val t = (time - xs[i]) / (xs[i + 1] - xs[i])
                return ys[i] + t * (ys[i + 1] - ys[i])
            }
        }

        return 0.0
    }
}
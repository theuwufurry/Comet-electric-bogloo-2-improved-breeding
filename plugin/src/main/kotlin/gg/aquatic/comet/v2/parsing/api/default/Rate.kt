package gg.aquatic.comet.v2.parsing.api.default

import kotlin.math.floor

class RateProvider(val rate: Double) {
    fun amount(time: Int): Int {
        val cumulative = floor((time + 1) * rate / 20.0)
        val previous = floor(time * rate / 20.0)
        return (cumulative - previous).toInt()
    }
}
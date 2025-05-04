package gg.aquatic.comet.api.js

import gg.aquatic.comet.api.emitter.environment.Datum
import kotlin.random.Random

/**
 * From min..max generates `amount` intervals and returns a random double from an interval
 */
object MathExtras {
    class IntervalRandom(
        private val min: Double,
        private val max: Double,
        private val amount: Int,
        private var count: Int = 0,
    ): Datum<IntervalRandom, IntervalRandom> {
        override val value = this

        private val range = max - min

        fun gen(): Double {
            return Random.nextDouble((range / amount * count) + min, (range / amount * ++count) + min)
        }

        override fun clone(): IntervalRandom {
            return IntervalRandom(
                min,
                max,
                amount,
                count
            )
        }
    }

    fun getIntervalRandom(
        min: Double,
        max: Double,
        amount: Int
    ): IntervalRandom {
        return IntervalRandom(min, max, amount)
    }
}
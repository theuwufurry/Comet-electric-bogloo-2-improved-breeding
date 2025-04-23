package gg.aquatic.comet.api.emitter.random

import java.util.*
import kotlin.random.Random

class DeterministicRandom(
    val seed: Int
) {
    val kotlinRandom = Random(seed)

    fun uuid(): UUID {
        val randomBytes = ByteArray(16)
        kotlinRandom.nextBytes(randomBytes)
        randomBytes[6] = (randomBytes[6].toInt() and 0x0f).toByte() /* clear version        */
        randomBytes[6] = (randomBytes[6].toInt() or 0x40).toByte() /* set to version 4     */
        randomBytes[8] = (randomBytes[8].toInt() and 0x3f).toByte() /* clear variant        */
        randomBytes[8] = (randomBytes[8].toInt() or 0x80.toByte().toInt()).toByte() /* set to IETF variant  */
        return UUID.nameUUIDFromBytes(randomBytes)
    }

    fun clone(): DeterministicRandom {
        return DeterministicRandom(seed)
    }
}
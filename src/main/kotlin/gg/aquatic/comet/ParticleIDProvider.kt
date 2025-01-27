package gg.aquatic.comet

import java.util.concurrent.atomic.AtomicInteger

object ParticleIDProvider {
    private val atomic = AtomicInteger()
    fun id(): Int = atomic.decrementAndGet()
}
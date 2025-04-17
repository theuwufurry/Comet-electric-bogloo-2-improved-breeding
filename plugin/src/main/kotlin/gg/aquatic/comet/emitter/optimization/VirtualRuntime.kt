package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.emitter.impl.OptimizedEmitter

class VirtualRuntime(
    initialEmitter: OptimizedEmitter
) {
    var t = 0
    private val emitters: MutableList<VirtualEmitter> = mutableListOf()
    private val emittersToAdd: MutableList<VirtualEmitter> = mutableListOf()

    init {
        emitters += VirtualEmitter(initialEmitter, this)
    }

    fun addEmitter(emitter: VirtualEmitter) {
        emittersToAdd += emitter
    }


    private var deathTime = -1

    /**
     * @return Whether the emitter should still be alive
     */
    fun step(realTime: Int): Boolean {
        if (deathTime != -1 && realTime >= deathTime) return false
        var iterations = 0
        while (iterations < MAX_ITERATIONS) {
            iterations++
            t++
            val deadEmitters: MutableList<VirtualEmitter> = mutableListOf()

            var remaining = false
            for (emitter in emitters) {
                val r = emitter.tick()
                if (!r.alive) {
                    deadEmitters += emitter
                    continue
                }

                if (!remaining && emitter.particleBirthTimes.values.any { it < realTime + LOOKAHEAD }) remaining = true
            }

            emitters.removeAll(deadEmitters)

            emitters += emittersToAdd
            emittersToAdd.clear()

            if (emitters.isEmpty()) {
                deathTime = t
                break
            }

            if (!remaining) break
        }

        return true
    }

    fun kill() {
        emitters.forEach { it.kill() }
    }

    companion object {
        const val LOOKAHEAD = 3
        const val MAX_ITERATIONS = 10_000
    }
}
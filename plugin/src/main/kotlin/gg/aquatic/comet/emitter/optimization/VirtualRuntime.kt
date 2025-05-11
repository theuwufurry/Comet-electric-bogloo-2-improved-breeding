package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.emitter.impl.OptimizedEmitter

class VirtualRuntime(
    private val initialEmitter: OptimizedEmitter
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
     * Spawn particles until reaching catchupTime
     */
    var catchupTime: Int? = null

    /*
    to spawn particles from a certain point and not spawn anything else, we also need to be able to jump back to a time

    at time T, process all particles that spawn within T + LOOKAHEAD ticks
    once all particles have been processed, we're done with that tick

    now, everything until T + LOOKAHEAD has been processed until FINISH_TIME, so we don't need to redo that
    go to T + LOOKAHEAD + 1

    don't rerun emitter components, for the times until FINISH_TIME
        cache the emitter datas for every time


    to keep runtime relative time,
        keep track of ticks, can't have centralized data on runtime since different particles might be at diff states
        pass along starting time to new virtual emitters
     */

    /**
     * @return Whether the emitter should still be alive
     */
    fun step(realTime: Int): Boolean {
        if (emitters.isEmpty()) return false

        catchupTime = realTime + initialEmitter.unrealizedEmitter.lookahead
//        println("RUNTIME: catchupTime:$catchupTime")
        if (deathTime != -1 && realTime >= deathTime) {
            return false
        }

        if (deathTime != -1) {
            return true
        }

        var iterations = 0
        while (iterations < MAX_ITERATIONS) {
//            println("== DOING VIRTUAL ITERATION ==")
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

                if (!remaining && emitter.particles.isNotEmpty()) remaining = true
            }

            emitters.removeAll(deadEmitters)

            emitters += emittersToAdd
            emittersToAdd.clear()

            if (emitters.isEmpty()) {
                return false
//                deathTime = t
//                break
            }

            if (!remaining) break
        }

        return true
    }

    fun kill() {
        emitters.forEach { it.kill() }
    }

    companion object {
        const val MAX_ITERATIONS = 10_000
    }
}
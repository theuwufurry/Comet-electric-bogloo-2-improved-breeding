package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.emitter.Emitter
import gg.aquatic.comet.emitter.GlobalTicker

class VirtualRuntime(
    initialEmitter: Emitter
) {
    /*
    after emitter dies, have it report back:
        - caches for Location and Display
        - the Path to travel
            - timestamped real world positions
        - timestamped display data
    put this in GlobalTicker registry in the same way
     */

    private val emitters: MutableList<VirtualEmitter> = mutableListOf()
    private val emittersToAdd: MutableList<VirtualEmitter> = mutableListOf()

    init {
        emitters += VirtualEmitter(initialEmitter, this)
    }

    fun addEmitter(emitter: VirtualEmitter) {
        emittersToAdd += emitter
    }

    fun generateCaches() {
        while (true) {
            val deadEmitters: MutableList<VirtualEmitter> = mutableListOf()

            for (emitter in emitters) {
                val r = emitter.tick()
                if (!r.alive) {
                    deadEmitters += emitter
                    GlobalTicker.emitterCache[emitter.emitterData.id] = emitter.cachedPath()
                }
            }

            emitters.removeAll(deadEmitters)

            emitters += emittersToAdd
            emittersToAdd.clear()

            if (emitters.isEmpty()) break
        }
    }
}
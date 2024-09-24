package com.ixume.particlesTesting.emitter.shape

import com.ixume.particlesTesting.emitter.EmitterMochaData
import org.joml.Vector3d

interface ShapeComponent {
    fun offset(emitterData: EmitterMochaData): Vector3d
}
package com.ixume.particleemitter.emitter.shape

import com.ixume.particleemitter.emitter.EmitterData
import org.joml.Vector3d
import javax.script.Bindings

interface ShapeComponent {
    fun offset(emitterData: EmitterData, emitterBindings: Bindings): Vector3d
}
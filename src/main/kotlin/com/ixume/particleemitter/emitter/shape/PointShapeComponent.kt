package com.ixume.particlesTesting.emitter.shape

import com.google.gson.JsonElement
import com.ixume.particlesTesting.emitter.Emitter
import com.ixume.particlesTesting.emitter.EmitterMochaData
import com.ixume.particlesTesting.emitter.EmitterMochaFunction
import com.ixume.particlesTesting.parsing.renameVariables
import javassist.LoaderClassPath
import org.joml.Vector3d
import team.unnamed.mocha.MochaEngine
import team.unnamed.mocha.runtime.MochaFunction

class PointShapeComponent(private val xOffset: EmitterMochaFunction, private val yOffset: EmitterMochaFunction, private val zOffset: EmitterMochaFunction) : ShapeComponent {
    companion object {
        fun parse(jsonElement: JsonElement): PointShapeComponent? {
            val mochaEngine = MochaEngine.createStandard()
            mochaEngine.classPool().appendClassPath(LoaderClassPath(EmitterMochaFunction::class.java.classLoader))
            val offsetVector = jsonElement.asJsonObject?.get("offset")?.asJsonArray ?: return null
            return PointShapeComponent(
                mochaEngine.compile(offsetVector.get(0)?.asString?.renameVariables() ?: return null, EmitterMochaFunction::class.java),
                mochaEngine.compile(offsetVector.get(1)?.asString?.renameVariables() ?: return null, EmitterMochaFunction::class.java),
                mochaEngine.compile(offsetVector.get(2)?.asString?.renameVariables() ?: return null, EmitterMochaFunction::class.java))
        }
    }

    override fun offset(emitterData: EmitterMochaData): Vector3d {
        return Vector3d(xOffset.eval(emitterData.age), yOffset.eval(emitterData.age), zOffset.eval(emitterData.age))
    }
}
package gg.aquatic.comet.snowstorm.deserialized.motion

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.position.MotionPositionComponent
import gg.aquatic.comet.particle.position.initial.InitialExpressionPositionComponent
import gg.aquatic.comet.particle.position.initial.SpherePositionComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.Program

class MotionDynamic(
    var initialOffset: Triple<Program?, Program?, Program?> = EMPTY_TRIPLE,
    var dir: Direction? = null,
    var magnitude: Program? = null,
    var linearAcceleration: Triple<Program?, Program?, Program?> = EMPTY_TRIPLE
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        if (initialOffset != Triple(null, null, null)) {
            val initialOffsetObj = JsonObject()
            initialOffset.first?.let { initialOffsetObj.addProperty("x", deserializedEffect.toJS(it)) }
            initialOffset.second?.let { initialOffsetObj.addProperty("y", deserializedEffect.toJS(it)) }
            initialOffset.third?.let { initialOffsetObj.addProperty("z", deserializedEffect.toJS(it)) }

            root["components"].asJsonObject.add(InitialExpressionPositionComponent.id, initialOffsetObj)
        }

        if (linearAcceleration != EMPTY_TRIPLE || dir != null) {
            val motionPositionObject = JsonObject()

            if (linearAcceleration != EMPTY_TRIPLE) {
                val accelerationObject = JsonObject()

                linearAcceleration.first?.let { accelerationObject.addProperty("x", deserializedEffect.toJS(it)) }
                linearAcceleration.second?.let { accelerationObject.addProperty("y", deserializedEffect.toJS(it)) }
                linearAcceleration.third?.let { accelerationObject.addProperty("z", deserializedEffect.toJS(it)) }

                motionPositionObject.add("acceleration", accelerationObject)
            }

            if (dir != null) {
                if (dir!! is Direction.VectorDirection) {
                    val vDir = dir!! as Direction.VectorDirection
                    val initialVelocityObj = JsonObject()

                    vDir.x?.let { initialVelocityObj.addProperty("x", deserializedEffect.toJS(it)) }
                    vDir.y?.let { initialVelocityObj.addProperty("y", deserializedEffect.toJS(it)) }
                    vDir.z?.let { initialVelocityObj.addProperty("z", deserializedEffect.toJS(it)) }
                    magnitude?.let { initialVelocityObj.addProperty("magnitude", deserializedEffect.toJS(it)) }

                    motionPositionObject.add("initial_velocity", initialVelocityObj)
                } else if (dir!! is Direction.SphereDirection) {
                    motionPositionObject.add("sphere_dir", JsonObject())
                    val spherePosObj = JsonObject()
                    val radius =
                        (deserializedEffect.components.first { it is SphereShapeDeserializedComponent } as SphereShapeDeserializedComponent).radius
                    val sphereDir = dir!! as Direction.SphereDirection

                    radius?.let { spherePosObj.addProperty("radius", deserializedEffect.toJS(it)) }
                    magnitude?.let { spherePosObj.addProperty("magnitude", deserializedEffect.toJS(it)) }
                    if (sphereDir.dir == SpherePositionComponent.DirType.INWARDS) {
                        spherePosObj.addProperty("direction", "inwards")
                    } else {
                        spherePosObj.addProperty("direction", "outwards")
                    }

                    root["components"].asJsonObject.add(SpherePositionComponent.id, spherePosObj)
                }
            }

            val deserializedSphere =
                (deserializedEffect.components.firstOrNull { it is SphereShapeDeserializedComponent } as? SphereShapeDeserializedComponent)
            if (deserializedSphere != null && (dir == null || dir!! !is Direction.SphereDirection)) {
                val spherePosObj = JsonObject()
                val radius =
                    (deserializedEffect.components.first { it is SphereShapeDeserializedComponent } as SphereShapeDeserializedComponent).radius

                radius?.let { spherePosObj.addProperty("radius", deserializedEffect.toJS(it)) }

                root["components"].asJsonObject.add(SpherePositionComponent.id, spherePosObj)
            }

            root["components"].asJsonObject.add(MotionPositionComponent.id, motionPositionObject)
        }
    }

    interface Direction {
        data class VectorDirection(
            val x: Program?,
            val y: Program?,
            val z: Program?,
        ) : Direction

        data class SphereDirection(
            val dir: SpherePositionComponent.DirType
        ) : Direction
    }

    companion object {
        val EMPTY_TRIPLE = Triple(null, null, null)
    }
}
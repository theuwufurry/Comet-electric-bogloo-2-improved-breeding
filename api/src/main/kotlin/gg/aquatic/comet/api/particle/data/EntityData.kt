package gg.aquatic.comet.api.particle.data

import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.display.DisplayData
import org.joml.Quaternionf
import org.joml.Vector3f

data class EntityData(
    val displayData: DisplayData,
    val color: Int,
    val transparency: Int,
    val reserveTransparency: Int?,
    val translation: Vector3f,
    val rotation: Quaternionf,
    val scale: Vector3f,
    val billboardConstraints: BillboardConstraints,
    val interpolationDelay: Int,
    val transformationInterpolationDuration: Int,
    val teleportationDuration: Int,
    val lightData: LightData?,
    val seeThrough: Boolean,
    val shadow: Boolean,
    val sensitiveCentering: Boolean,
) {
    fun copy(): EntityData {
        return EntityData(
            displayData.copy(),
            color,
            transparency,
            reserveTransparency,
            Vector3f(translation),
            Quaternionf(rotation),
            Vector3f(scale),
            billboardConstraints,
            interpolationDelay,
            transformationInterpolationDuration,
            teleportationDuration,
            lightData,
            seeThrough,
            shadow,
            sensitiveCentering,
        )
    }
}
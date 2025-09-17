package gg.aquatic.comet.api.particle

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.VariableMutableMap
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*

data class ParticleData(
    var id: UUID,
    var particle: AbstractParticle? = null,
    var dead: Boolean = false,
    var age: Double = 0.0,
    var maxLife: Int = 0,
    var displayData: DisplayData<*> = SpriteData(""),
    var color: Int = 0,
    var origin: Vector3d = Vector3d(),
    var relativePosition: Vector3d = Vector3d(),
    var translation: Vector3f = Vector3f(),
    var rotation: Quaternionf = Quaternionf(),
    var scale: Vector3f = Vector3f(),
    var velocity: Vector3d = Vector3d(),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    var emitter: AbstractEmitter? = null,
    var acceleration: Vector3d = Vector3d(),
    var interpolationDelay: Int = 0,
    var transformationInterpolationDuration: Int = 2,
    var teleportationDuration: Int = 1,
    var light: LightData? = null,
    var seeThrough: Boolean = false,
    var shadow: Boolean = false,
    var sensitiveCentering: Boolean = true,
) {
    val pos: Vector3d
        get() = Vector3d(origin).add(relativePosition)

    val max_life: Int by ::maxLife

    fun copyFrom(other: ParticleData) {
        id = other.id
        particle = other.particle
        dead = other.dead
        age = other.age
        maxLife = other.maxLife
        displayData = other.displayData
        color = other.color
        origin = other.origin
        relativePosition = other.relativePosition
        translation = other.translation
        rotation = other.rotation
        scale = other.scale
        velocity = other.velocity
        billboardConstraints = other.billboardConstraints
        acceleration = other.acceleration
        interpolationDelay = other.interpolationDelay
        transformationInterpolationDuration = other.transformationInterpolationDuration
        teleportationDuration = other.teleportationDuration
        light = other.light
        seeThrough = other.seeThrough
        shadow = other.shadow
        sensitiveCentering = other.sensitiveCentering

        variable = other.variable
    }

    var variable: MutableMap<String, Any> = VariableMutableMap()
    val externalVariable: MutableMap<String, Any> = EmitterData.MapWrapper(::variable)

    fun locHash(): Int {
        return arrayOf(origin, relativePosition).contentHashCode()
    }

    fun displayHash(): Int {
        return arrayOf(displayData, color, rotation, scale).contentHashCode()
    }

    fun clone(): ParticleData {
        return ParticleData(
            id = id,
            particle = particle,
            dead = dead,
            age = age,
            maxLife = maxLife,
            displayData = displayData.copy(),
            color = color,
            origin = Vector3d(origin),
            relativePosition = Vector3d(relativePosition),
            translation = Vector3f(translation),
            rotation = Quaternionf(rotation),
            scale = Vector3f(scale),
            velocity = Vector3d(velocity),
            billboardConstraints = billboardConstraints,
            emitter = emitter,
            acceleration = Vector3d(acceleration),
            interpolationDelay = interpolationDelay,
            transformationInterpolationDuration = transformationInterpolationDuration,
            teleportationDuration = teleportationDuration,
            light = light,
            seeThrough = seeThrough,
            shadow = shadow,
            sensitiveCentering = sensitiveCentering,
        ).apply i@{
            this@i.variable = (this@ParticleData.variable as VariableMutableMap).clone()
        }
    }
}

data class LightData(val skylight: Int, val blocklight: Int)
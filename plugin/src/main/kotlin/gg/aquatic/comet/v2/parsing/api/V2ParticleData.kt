package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*

data class V2ParticleData(
    val effect: Effect,
    @JvmField @HostAccess.Export val uuid: UUID = UUID.randomUUID(),
    @JvmField @HostAccess.Export var age: Int = 0,
    @JvmField @HostAccess.Export var displayData: DisplayData<*> = SpriteData(""),
    @JvmField @HostAccess.Export var color: Int = -1,
    @JvmField @HostAccess.Export var translation: Vector3f = Vector3f(),
    @JvmField @HostAccess.Export var rotation: Quaternionf = Quaternionf(),
    @JvmField @HostAccess.Export var scale: Vector3f = Vector3f(1f),
    @JvmField @HostAccess.Export var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    @JvmField @HostAccess.Export var interpolationDelay: Int = 0,
    @JvmField @HostAccess.Export var transformationInterpolationDuration: Int = 2,
    @JvmField @HostAccess.Export var teleportationDuration: Int = 1,
    @JvmField @HostAccess.Export var light: LightData? = null,
    @JvmField @HostAccess.Export var seeThrough: Boolean = false,
    @JvmField @HostAccess.Export var shadow: Boolean = false,
    @JvmField @HostAccess.Export var sensitiveCentering: Boolean = false,
    @JvmField @HostAccess.Export var origin: Vector3d = Vector3d(),
    @JvmField @HostAccess.Export var relativePosition: Vector3d = Vector3d(),
    @JvmField @HostAccess.Export val data: Value = effect.api.context.eval("js", "({})"),
) {
    @HostAccess.Export
    fun spawn() {
        effect.spawnParticle(this)
    }
}
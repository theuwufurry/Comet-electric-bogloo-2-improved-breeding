package gg.aquatic.comet.api

import gg.aquatic.comet.api.CometRegistry.unrealizedEmitterByID
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector2f
import java.util.function.Consumer
import java.util.function.Supplier

class CometEffectBuilder(
    val id: String,
) {
    private var pose: Pose? = null

    fun pose(pose: Pose): CometEffectBuilder {
        this.pose = pose
        return this
    }

    fun location(location: Location): CometEffectBuilder {
        this.pose = location.pose()
        return this
    }

    private var parent: Parent? = null

    fun parent(parent: Parent): CometEffectBuilder {
        this.parent = parent
        return this
    }

    private var environmentData: EnvironmentData = EnvironmentData()

    fun environmentData(
        size: Double,
        data: Map<String, Any>
    ): CometEffectBuilder {
        environmentData = EnvironmentData.create(size, data)
        return this
    }

    private var audience: AquaticAudience = GlobalAudience()

    fun audience(audience: AquaticAudience): CometEffectBuilder {
        this.audience = audience
        return this
    }

    private var mount: Mount? = null

    fun mount(mount: Mount): CometEffectBuilder {
        this.mount = mount
        return this
    }

    private var yawpitchSupplier: Supplier<Vector2f>? = null

    fun yawpitchSupplier(yawpitchSupplier: Supplier<Vector2f>): CometEffectBuilder {
        this.yawpitchSupplier = yawpitchSupplier
        return this
    }

    private var after: Consumer<AbstractEmitter> = Consumer {  }

    fun after(after: Consumer<AbstractEmitter>): CometEffectBuilder {
        this.after = after
        return this
    }

    /**
     * @return False if emitter not found by ID, true on success.
     */
    fun spawn(): Boolean {
        require(pose != null) { "Must set effect's pose via the pose() or location() method."}
        (unrealizedEmitterByID(id) ?: return false).realize(
            parent, pose!!, environmentData, audience, mount, yawpitchSupplier, after
        )

        return true
    }
}
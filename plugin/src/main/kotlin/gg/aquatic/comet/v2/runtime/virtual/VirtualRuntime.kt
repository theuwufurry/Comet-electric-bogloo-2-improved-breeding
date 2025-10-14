package gg.aquatic.comet.v2.runtime.virtual

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.EffectRuntimeProxy
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.audience.Audience
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.bukkit.entity.Player
import java.util.UUID

class VirtualRuntime(
    val uuid: UUID,
    val relPose: Pose,
    val api: JSEffectAPI,
    var parent: Parent?,
    val audience: Audience,
    val data: JsonElement,
) : EffectRuntime {
    override val proxy: EffectRuntimeProxy = EffectRuntimeProxy(this)
    val timestampedExecutables = Int2ObjectOpenHashMap<MutableList<BoundExecutable>>().apply {
        defaultReturnValue(null)
    }

    val effect = VirtualEffect(
        uuid = uuid,
        relPose = relPose,
        runtime = this,
        api = api,
        parent = parent,
        audience = audience,
        data = data,
    )

    init {
        effect.init()
    }

    fun tick() {
        effect.tick()
    }

    /*
    since an executable is submitted by simulated particles, their time is hard to predict; check VirtualEffect's status.
     */
    override fun submitExecutable(boundExecutable: BoundExecutable) {
        val status = effect.status
        when (status) {
            is Status.Effect -> timestampedExecutables.getOrPut(status.time) { mutableListOf() } += boundExecutable
            is Status.Particle -> timestampedExecutables.getOrPut(status.time) { mutableListOf() } += boundExecutable
            Status.Other -> throw IllegalStateException()
        }
    }

    fun onKill() {
        effect.onKill()
    }

    override fun remove(effect: Effect) {
        check(this.effect == effect)
    }
}
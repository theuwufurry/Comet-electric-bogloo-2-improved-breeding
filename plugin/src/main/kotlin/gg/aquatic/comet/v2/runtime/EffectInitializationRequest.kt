package gg.aquatic.comet.v2.runtime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.runtime.emitter.Effect
import java.util.function.Consumer

/*
parent: Parent?,
pose: Pose,
environmentData: EnvironmentData,
audience: AquaticAudience,
mount: Mount?,
yawpitchSupplier: Supplier<YawPitch>?,
after: Consumer<AbstractEmitter>,
 */
class EffectInitializationRequest(
    val unrealized: JSEffectAPI,
    val pose: Pose,
    val parent: Parent?,
    val data: JsonElement,
    val after: Consumer<Effect>
)
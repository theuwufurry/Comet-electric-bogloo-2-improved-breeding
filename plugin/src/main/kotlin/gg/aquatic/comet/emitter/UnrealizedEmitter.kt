package gg.aquatic.comet.emitter


import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.Mount
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.YawPitch
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.impl.Emitter
import gg.aquatic.comet.emitter.impl.MountedUnoptimizedEmitter
import gg.aquatic.comet.emitter.impl.OptimizedEmitter
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector3d
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

/*
mNO: optimized : 1 : 2
mNo: basic not optimized : 2
mnO: basic optimized
mno: basic not optimized
MNO: optimized : 1 : 2
MNo: basic not optimized : 1 : 2
MnO: optimized : 1
Mno: unoptimized : 1

technologies needed:
1 convert teleport to translation
2 add rot to tp packets
 */

data class UnrealizedEmitter(
    override val id: String,
    override val preInitComponents: List<PreInitComponent>,
    override val components: List<Component>,
    val rateComponent: RateComponent,
    val distanceCullingComponent: DistanceCullingComponent,
    val updateFrequencyComponent: UpdateFrequencyComponent,
    override val billboardConstraints: BillboardConstraints,
    val isListed: Boolean,
    override val isDoubleSided: Boolean,
    override val lookahead: Int,
    override val persistent: Boolean,
) : AbstractUnrealizedEmitter() {
    override fun realize(
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        mount: Mount?,
        yawpitchSupplier: Supplier<YawPitch>?,
        after: Consumer<AbstractEmitter>,
    ) {
        val initialization = {
            val emitterData = EmitterData(UUID.randomUUID())
            emitterData.world = pose.world
            emitterData.location = pose.location
            preInitComponents.forEach { it.init(emitterData, environmentData) }
            emitterData.variable.putAll(environmentData.data)

            val optimized = checkOptimized(environmentData)
            val mounted = mount != null

            val emitter: AbstractEmitter = if (mounted) {
                MountedUnoptimizedEmitter(
                    parent,
                    components,
                    rateComponent,
                    distanceCullingComponent,
                    updateFrequencyComponent,
                    billboardConstraints,
                    pose,
                    emitterData,
                    this,
                    environmentData,
                    audience,
                    mount = mount,
                    yawpitchSupplier = yawpitchSupplier
                )
            } else {
                if (optimized) OptimizedEmitter(
                    parent,
                    components,
                    rateComponent,
                    distanceCullingComponent,
                    billboardConstraints, pose, emitterData, this, environmentData, audience, false, yawpitchSupplier = yawpitchSupplier
                ) else Emitter(
                    parent,
                    components,
                    rateComponent,
                    distanceCullingComponent,
                    updateFrequencyComponent,
                    billboardConstraints, pose, emitterData, this, environmentData, audience, yawpitchSupplier = yawpitchSupplier
                )

            }

            after.accept(emitter)

            emitter
        }

        GlobalTicker.addInitialization(initialization)
    }

    private fun checkOptimized(data: EnvironmentData): Boolean {
        return !(data.data["optimize"] != null && data.data["optimize"] is Boolean && data.data["optimize"] == false)
    }

    override fun realize(
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        mount: Mount?,
        yawpitchSupplier: Supplier<YawPitch>?,
        after: Consumer<AbstractEmitter>,
    ) {
//        println("a pose: $pose")
        realize(parent, pose, environmentData, GlobalAudience(), mount, yawpitchSupplier, after)
    }

    override fun internalRealize(
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID,
        mount: Mount?,
        yawpitchSupplier: Supplier<YawPitch>?
    ): AbstractEmitter {
        val emitterData = EmitterData(uuid)
        emitterData.world = pose.world
        emitterData.location = pose.location
        preInitComponents.forEach { it.init(emitterData, environmentData) }
        emitterData.variable.putAll(environmentData.data)

        val optimized = checkOptimized(environmentData)
        val mounted = mount != null

        val e = if (mounted) {
            MountedUnoptimizedEmitter(
                parent,
                components,
                rateComponent,
                distanceCullingComponent,
                updateFrequencyComponent,
                billboardConstraints,
                pose,
                emitterData,
                this,
                environmentData,
                audience,
                seed = random.kotlinRandom.nextInt(),
                mount!!,
                yawpitchSupplier
            )
        } else if (optimized) OptimizedEmitter(
            parent,
            components,
            rateComponent,
            distanceCullingComponent,
            billboardConstraints,
            pose,
            emitterData,
            this,
            environmentData,
            audience,
            true,
            seed = random.kotlinRandom.nextInt(),
            yawpitchSupplier
        ) else Emitter(
            parent,
            components,
            rateComponent,
            distanceCullingComponent,
            updateFrequencyComponent,
            billboardConstraints,
            pose,
            emitterData,
            this,
            environmentData,
            audience,
            seed = random.kotlinRandom.nextInt(),
            yawpitchSupplier = yawpitchSupplier
        )

        GlobalTicker.addEmitter(e)

        return e
    }

    fun virtualRealize(
        parent: Parent?,
        pose: Pose,
        environmentData: EnvironmentData,
        random: DeterministicRandom,
        runtime: VirtualRuntime,
        uuid: UUID,
        timeOffset: Int,
        mount: Mount?,
        yawpitchSupplier: Supplier<YawPitch>?,
    ) {
        val emitterData = EmitterData(uuid)
        emitterData.world = pose.world
        emitterData.location = pose.location
        preInitComponents.forEach { it.init(emitterData, environmentData) }
        emitterData.variable.putAll(environmentData.data)

        val optimized = checkOptimized(environmentData)
        if (!optimized) {
            return
        }

        runtime.addEmitter(
            VirtualEmitter(
                parent = parent,
                unrealizedEmitter = this,
                runtime = runtime,
                rateComponent = rateComponent,
                components = components,
                billboardConstraints = billboardConstraints,
                backerPose = pose,
                backerEmitterData = emitterData,
                backerEnvironmentData = environmentData,
                seed = random.kotlinRandom.nextInt(),
                timeOffset,
                mount = mount,
                yawpitchSupplier = yawpitchSupplier
            )
        )
    }
}
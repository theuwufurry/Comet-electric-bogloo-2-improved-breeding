package gg.aquatic.comet.v2.runtime.virtual

class OptimizationSettings(
    @JvmField var enabled: Boolean,
    @JvmField var positionTolerance: Double,
    @JvmField var scaleTolerance: Double,
    @JvmField var colorTolerance: Double,
    @JvmField var rotTolerance: Double,
    @JvmField var opacityTolerance: Double,
    @JvmField var interval: Int,
)
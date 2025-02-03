package gg.aquatic.comet.api.particle.data

import org.joml.Quaternionf
import org.joml.Vector3f

enum class BillboardConstraints(val byte: Byte) {
    FIXED((0).toByte()),
    VERTICAL((1).toByte()),
    HORIZONTAL((2).toByte()),
    CENTER((3).toByte())
}
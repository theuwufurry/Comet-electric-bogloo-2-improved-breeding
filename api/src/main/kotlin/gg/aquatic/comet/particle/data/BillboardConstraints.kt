package gg.aquatic.comet.particle.data

enum class BillboardConstraints(val byte: Byte) {
    FIXED((0).toByte()),
    VERTICAL((1).toByte()),
    HORIZONTAL((2).toByte()),
    CENTER((3).toByte())
}
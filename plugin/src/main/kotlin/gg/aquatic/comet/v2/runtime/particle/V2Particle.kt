package gg.aquatic.comet.v2.runtime.particle

import com.github.retrooper.packetevents.wrapper.PacketWrapper

interface V2Particle {
    val id: Int
    fun getAddPacket(): List<PacketWrapper<*>>
}
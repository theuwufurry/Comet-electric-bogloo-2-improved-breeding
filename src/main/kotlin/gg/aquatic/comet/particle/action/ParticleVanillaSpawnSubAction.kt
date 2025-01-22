package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.Particle
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.type.ParticleTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle

class ParticleVanillaSpawnSubAction(
//    private val myParticleData: ParticleData,
//    private val myEmitterData: EmitterData
) : SubAction {
    override fun execute(context: ActionContext) {
        val playerManager = PacketEvents.getAPI().playerManager
        context.pos ?: return

        val particlePacket = WrapperPlayServerParticle(
            Particle(ParticleTypes.NOTE),
            false,
            Vector3d(
                context.pos.x,
                context.pos.y,
                context.pos.z
            ),
            Vector3f.zero(),
            0f,
            1
        )

        context.otherEmitterData.emitter!!.players().forEach { playerManager.sendPacket(it, particlePacket) }
    }

    companion object : ComponentParser<ParticleVanillaSpawnSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleVanillaSpawnSubAction? {
            return if (jsonElement.isJsonPrimitive && jsonElement.asJsonPrimitive.isString && jsonElement.asJsonPrimitive.asString == "vanilla_particle") ParticleVanillaSpawnSubAction() else null
        }
    }
}
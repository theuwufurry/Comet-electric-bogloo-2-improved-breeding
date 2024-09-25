package com.ixume.particleemitter.particle

import com.ixume.particleemitter.ParticleIDProvider
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Display.TextDisplay
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.bukkit.Color
import org.bukkit.World
import org.bukkit.craftbukkit.CraftWorld
import org.joml.Vector3d
import java.util.UUID
import javax.script.Bindings
import javax.script.SimpleBindings

class Particle(private var location: Vector3d) {
    val data: ParticleData = ParticleData(0.0)
    val bindings: Bindings = SimpleBindings(mapOf("particle" to data))

    val id = ParticleIDProvider.id
    private val uuid = UUID.randomUUID()

    fun tick() {
        data.age++
    }

    fun getAddPacket(world: World): net.minecraft.network.protocol.Packet<*> {
        val level = (world as CraftWorld).handle
        val entity = TextDisplay(EntityType.TEXT_DISPLAY, level)
        entity.text = Component.literal("P")
        entity.billboardConstraints = Display.BillboardConstraints.CENTER
        entity.entityData.set(TextDisplay.DATA_BACKGROUND_COLOR_ID, Color.fromARGB(0, 0, 0, 0).asARGB())
        val packet = ClientboundAddEntityPacket(id, uuid, location.x, location.y, location.z, 0F, 0F, EntityType.TEXT_DISPLAY, 0, Vec3(0.0, 0.0, 0.0), 0.0)
        val entityDataPacket = entity.entityData.nonDefaultValues?.let { ClientboundSetEntityDataPacket(id, it) }
        return ClientboundBundlePacket(listOf(packet, entityDataPacket))
    }
}
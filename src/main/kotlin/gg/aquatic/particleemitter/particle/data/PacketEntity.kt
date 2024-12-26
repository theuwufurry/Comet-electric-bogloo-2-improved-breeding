package gg.aquatic.particleemitter.particle.data

import com.ixume.particleemitter.particle.Particle
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class PacketEntity(type: EntityType<*>, world: Level) : Entity(type, world) {
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}

    override fun addAdditionalSaveData(nbt: CompoundTag) {}

    override fun readAdditionalSaveData(nbt: CompoundTag) {}

    var particle: Particle? = null

    override fun getId(): Int {
        return particle!!.id
    }

    override fun trackingPosition(): Vec3 {
        return Vec3(particle!!.origin.x + particle!!.data.relativePosition.x, particle!!.origin.y + particle!!.data.relativePosition.y, particle!!.origin.z + particle!!.data.relativePosition.z)
    }

    override fun getYRot(): Float {
        return 0F
    }

    override fun getXRot(): Float {
        return 0F
    }

    override fun onGround(): Boolean {
        return false
    }
}
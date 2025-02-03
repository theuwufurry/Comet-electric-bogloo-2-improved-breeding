package gg.aquatic.comet.emitter

import gg.aquatic.comet.emitter.environment.EnvironmentData
import gg.aquatic.comet.emitter.parent.Parent
import gg.aquatic.comet.emitter.parent.Pose
import gg.aquatic.comet.emitter.parent.pose
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.concurrent.ConcurrentHashMap

class Emitter(
    location: Location,
    val forwardVector: Vector3d,
    val environmentData: EnvironmentData,
) : Parent {

    var location = location
        private set

    //origin can change, rotation can change
    private var blocked = false
    private var dead = false
    var emitterRotation: Quaterniond = calculateEmitterRotation()

    fun calculateEmitterRotation(): Quaterniond {
        return Quaterniond().rotateTo(forwardVector, pose().dir)
    }

    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()


    fun setPose(pose: Pose) {
        location.x = pose.pos.x
        location.y = pose.pos.y
        location.z = pose.pos.z

        location.direction = Vector(
            pose.dir.x,
            pose.dir.y,
            pose.dir.z
        )
    }

    override fun pose(): Pose {
        return location.pose()
    }

    fun kill() {
    }

    fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return Quaternionf()
    }
}
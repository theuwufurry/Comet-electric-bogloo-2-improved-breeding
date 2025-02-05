package gg.aquatic.comet.hook.modelengine

import com.ticxo.modelengine.api.model.bone.ModelBone
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose

class MEGParent(
    val bone: ModelBone,
): Parent {
    override val pose: Pose
        get() {
            return bone.location.pose()
        }
}
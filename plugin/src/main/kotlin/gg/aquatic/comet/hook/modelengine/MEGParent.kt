package gg.aquatic.comet.hook.modelengine

import com.ticxo.modelengine.api.model.ActiveModel
import com.ticxo.modelengine.api.model.bone.ModelBone
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose

interface MEGParent : Parent {
    companion object {

        fun byBone(bone: ModelBone): MEGBoneParent {
            return MEGBoneParent(bone)
        }
        fun byModel(model: ActiveModel): MEGModelParent {
            return MEGModelParent(model)
        }
    }

    class MEGBoneParent(private val bone: ModelBone) : MEGParent {
        override val pose: Pose
            get() {
                return bone.location.pose()
            }

        override val dead: Boolean = true
    }

    class MEGModelParent(private val model: ActiveModel) : MEGParent {
        override val pose: Pose
            get() {
                return model.modeledEntity.base.location.pose()
            }
        override val dead: Boolean = true
    }
}
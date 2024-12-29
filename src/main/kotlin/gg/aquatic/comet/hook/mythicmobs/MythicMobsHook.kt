package gg.aquatic.comet.hook.mythicmobs

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.hook.IHook
import io.lumine.mythic.core.skills.CustomComponentRegistry

object MythicMobsHook: IHook {
    override fun initialize() {
        val componentPackages = ArrayList<String>()
        componentPackages += "gg.aquatic.comet.hook.mythicmobs.mechanics"

        CustomComponentRegistry(ParticleEmitter.INSTANCE, componentPackages)
    }


}
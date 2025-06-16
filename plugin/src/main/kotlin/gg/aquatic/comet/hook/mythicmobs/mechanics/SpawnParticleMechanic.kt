package gg.aquatic.comet.hook.mythicmobs.mechanics

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.util.audience.FilterAudience
import io.lumine.mythic.api.adapters.AbstractLocation
import io.lumine.mythic.api.skills.ITargetedLocationSkill
import io.lumine.mythic.api.skills.SkillMetadata
import io.lumine.mythic.api.skills.SkillResult
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent

class SpawnParticleMechanic(
    plugin: ParticleEmitter,
    loader: MythicMechanicLoadEvent
) : ITargetedLocationSkill {

    private val audience = loader.config.getAudience("audience", "CASTER")
    private val emitterId = loader.config.getPlaceholderString(arrayOf("emitter", "e"), "")

    override fun castAtLocation(skillMeta: SkillMetadata, aLocation: AbstractLocation): SkillResult {
        val audience = this.audience.get(skillMeta, skillMeta.trigger)

        val players = audience.mapNotNull { BukkitAdapter.adapt(it) }
        val aquaticAudience = FilterAudience { player -> players.contains(player) }

        val location = BukkitAdapter.adapt(aLocation)

        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[emitterId.get()]
        emitter?.realize(
            parent = null,
            pose = location.pose(),
            audience = aquaticAudience
        ) {}

        return SkillResult.SUCCESS
    }
}
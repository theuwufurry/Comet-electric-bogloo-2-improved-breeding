# TODO - Feature List

Variables:
- On emitter initialization, to be used throughout the emitter's lifetime
- Emitter variables can be updated per emitter tick
- On particle initialization, to be cached throughout particles's lifetime
- Particle variabels can be updated per particle tick
- Variables passed to sub-emitters

Events Triggers:
- On Emitter Spawn
- On Particle Spawn
- On Emitter Tick
- On Particle Tick
- On Collision
- On Expression

Actions:
- Kill particle
- Kill emitter
- spawn sub-emitter
- variable update
- change texture?

Misc:
- CREATE A WIKI
- "Once" emitter spawn rate component
- per tick emitter spawn rate component
- map based emitter spawn rate component
- randomized display & color component
- Default components
- Global macros
- Standardize vector input
- Switch to YML
- Remove need for escaped quotes to return a string
- Handle input as Number not Double
- Emitter max lifetime (like particle max life time) component
- Informative errors
- Config-based colors (no more 'new Color()' shenanigans)
- Emitter layering
- Localized / Global movement
- Folders for different emitter configs
- Premade assets.

Performance:
- Make components constant if they dont have any changing variables (AHEM GRADIENT COMPONENT AHEM)
- Smart caching with once-per-lifetime evaluations
- Cache entire particle systems

Bugs:
- Particles randomly teleporting incorrectly when too many. Sync issue?

// edit "improved breeding fork"

removed all implmentation of mithic mobs and whatever
hard coded all apis made by ixume
removed waves
updated to latest version 1.21.10


//

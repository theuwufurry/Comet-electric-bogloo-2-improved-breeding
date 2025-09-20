package gg.aquatic.comet.api.parsing.resourcepack.packages

import gg.aquatic.comet.api.parsing.resourcepack.Model
import gg.aquatic.comet.api.parsing.resourcepack.Texture
import java.io.File

class Package(
    val name: String,
    val textures: List<Texture>,
    val models: List<Model>,
    val effects: List<File>,
)
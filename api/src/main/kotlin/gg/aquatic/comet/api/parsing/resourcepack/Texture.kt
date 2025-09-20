package gg.aquatic.comet.api.parsing.resourcepack

import java.io.File

class Texture(
    val namespace: String?,
    val file: File,
) {
    val name = namespace?.let { "${it}.${file.name}" } ?: file.name
    val nameWithoutExtension = namespace?.let { "${it}.${file.nameWithoutExtension}" } ?: file.nameWithoutExtension
}
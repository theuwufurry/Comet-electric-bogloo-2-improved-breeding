package gg.aquatic.comet.api.parsing.resourcepack

import java.io.File

class Model (
    val namespace: String?,
    val file: File,
) {
    val name = namespace?.let { "${it}.${file.name}" } ?: file.name
    val nameWithoutExtension = namespace?.let { "${it}.${file.nameWithoutExtension}" } ?: file.nameWithoutExtension
    val modelName = file.nameWithoutExtension
}
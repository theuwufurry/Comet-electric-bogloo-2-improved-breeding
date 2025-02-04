package gg.aquatic.comet.particle.color

fun String.addDependency(): String {
    if (contains("Color")) {
        return "var Color = Java.type('java.awt.Color');$this"
    }

    return this
}

interface ColorComponent
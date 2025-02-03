package gg.aquatic.comet.particle.display

interface DisplayComponent

interface DisplayData {
    fun copy(): DisplayData
}

data class TextDisplayComponent(val string: String) : DisplayData {
    override fun copy(): DisplayData {
        return TextDisplayComponent(string)
    }
}
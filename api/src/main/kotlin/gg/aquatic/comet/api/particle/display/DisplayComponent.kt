package gg.aquatic.comet.api.particle.display

interface DisplayComponent

interface DisplayData {
    fun copy(): DisplayData
}

data class TextData(
    val string: String,
    val backgroundColor: Int,
    val lineWidth: Int,
) : DisplayData {
    override fun copy(): DisplayData {
        return TextData(string, backgroundColor, lineWidth)
    }
}
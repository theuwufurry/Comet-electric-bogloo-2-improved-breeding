package gg.aquatic.comet.snowstorm.transpilation.token

class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any? = null,
    val line: Int
) {
    override fun toString(): String {
        return "$type $lexeme $literal"
    }
}
package gg.aquatic.comet.snowstorm.transpilation.token

enum class TokenType {
    NUMBER, IDENTIFIER, STRING,

    BANG, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,
    EQUAL_EQUAL,

    STAR, SLASH, PLUS, MINUS,

    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    SEMICOLON, COMMA, DOT,

    IF, ELSE, TRUE, FALSE,

    MATH,

    EOF
}
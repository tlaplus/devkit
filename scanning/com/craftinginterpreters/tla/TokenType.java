package com.craftinginterpreters.tla;

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  LEFT_BRACKET, RIGHT_BRACKET, COMMA, COLON,
  MINUS, PLUS, LESS_THAN, NEGATION, PRIME,

  // Short fixed-length tokens.
  EQUAL, EQUAL_EQUAL, AND, OR, DOT_DOT, ALL_MAP_TO,

  // Literals.
  IDENTIFIER, NUMBER, TRUE, FALSE,

  // Keywords.
  VARIABLES, ENABLED, IF, THEN, ELSE,

  // Symbols.
  IN, EXISTS, FOR_ALL,

  EOF
}


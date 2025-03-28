package com.craftinginterpreters.tla;

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, MINUS, PLUS, EQUALS, LESS_THAN, NEGATION, PRIME,

  // One or two character tokens.
  AND, OR, DEF_EQ, IN,

  // Literals.
  IDENTIFIER, NAT_NUMBER, TRUE, FALSE,

  // Keywords.
  VARIABLES, ENABLED, IF, THEN, ELSE,
  SINGLE_LINE, MODULE, DOUBLE_LINE,

  EOF
}


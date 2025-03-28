package com.craftinginterpreters.tla;

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, MINUS, PLUS, LESS_THAN, NEGATION, PRIME,

  // One or two character tokens.
  AND, OR, EQUALS, DEF_EQ,

  // Literals.
  IDENTIFIER, NAT_NUMBER, TRUE, FALSE,

  // Keywords.
  VARIABLES, ENABLED, IF, THEN, ELSE, IN,
  SINGLE_LINE, MODULE, DOUBLE_LINE,

  EOF
}


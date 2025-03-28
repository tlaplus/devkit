package com.craftinginterpreters.tla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.tla.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private int column = 0;

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line, column));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '+': addToken(PLUS); break;
      case '<': addToken(LESS_THAN); break;
      case '~': addToken(NEGATION); break;
      case '\'': addToken(PRIME); break;
      case '-':
        if (match('-')) {
          if ('-' == peek() && '-' == peekNext()) {
            while (match('-')) advance();
            addToken(SINGLE_LINE);
          } else {
            TlaPlus.error(line, "Unexpected character.");
          }
        } else {
          addToken(MINUS);
        }
        break;
      case '=':
        if (match('=')) {
          if ('=' == peek() && '=' == peekNext()) {
            while (match('=')) advance();
            addToken(DOUBLE_LINE);
          } else {
            addToken(DEF_EQ);
          }
        } else {
          addToken(EQUALS);
        }
        break;
      case '\\':
        if (match('/')) {
          addToken(OR);
        } else if (match('*')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('i') && match('n')) {
          addToken(IN);
        } else {
          TlaPlus.error(line, "Unexpected character.");
        }
        break;
      case '/':
        if (match('\\')) {
          addToken(AND);
        } else {
          TlaPlus.error(line, "Unexpected character.");
        }
        break;

      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;
 
      case '\n':
        column = 0;
        line++;
        break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          TlaPlus.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("ELSE",       ELSE);
    keywords.put("ENABLED",    ELSE);
    keywords.put("FALSE",      FALSE);
    keywords.put("IF",         IF);
    keywords.put("MODULE",     MODULE);
    keywords.put("THEN",       THEN);
    keywords.put("TRUE",       TRUE);
    keywords.put("VARIABLE",   VARIABLES);
    keywords.put("VARIABLES",  VARIABLES);
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    while (isDigit(peek())) advance();
    addToken(NAT_NUMBER,
        Integer.parseInt(source.substring(start, current)));
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    column++;
    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char advance() {
    column++;
    return source.charAt(current++);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line, column));
  }
}


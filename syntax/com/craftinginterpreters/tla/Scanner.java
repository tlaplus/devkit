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
      case '[': addToken(LEFT_BRACKET); break;
      case ']': addToken(RIGHT_BRACKET); break;
      case ',': addToken(COMMA); break;
      case ':': addToken(COLON); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case '<': addToken(LESS_THAN); break;
      case '~': addToken(NEGATION); break;
      case '\'': addToken(PRIME); break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '\\':
        if (match('/')) {
          addToken(OR);
        } else if (match('*')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (isAlpha(peek())) {
          symbol();
        } else {
          TlaPlus.error(line, "Unexpected character.");
        }
        break;
      case '/':
        if (consume('\\')) addToken(AND);
        break;
      case '.':
        if (consume('.')) addToken(DOT_DOT);
        break;
      case '|':
        if (consume('-') && consume('>')) addToken(ALL_MAP_TO);
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

  private static final Map<String, TokenType> symbols;

  static {
    symbols = new HashMap<>();
    symbols.put("\\land",     AND);
    symbols.put("\\E",        EXISTS);
    symbols.put("\\exists",   EXISTS);
    symbols.put("\\A",        FOR_ALL);
    symbols.put("\\forall",   FOR_ALL);
    symbols.put("\\in",       IN);
    symbols.put("\\lnot",     NEGATION);
    symbols.put("\\neg",      NEGATION);
    symbols.put("\\lor",      OR);
  }

  private void symbol() {
    while (isAlpha(peek())) advance();

    String text = source.substring(start, current);
    TokenType type = symbols.get(text);
    if (type == null) TlaPlus.error(line, "Unexpected character.");
    else addToken(type);
  }

  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("ELSE",       ELSE);
    keywords.put("ENABLED",    ENABLED);
    keywords.put("FALSE",      FALSE);
    keywords.put("IF",         IF);
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

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    while (isDigit(peek())) advance();
    addToken(NAT_NUMBER,
        Integer.parseInt(source.substring(start, current)));
  }

  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    column++;
    current++;
    return true;
  }

  private boolean consume(char expected) {
    if (match(expected)) return true;
    else TlaPlus.error(line, "Unexpected character.");
    return false;
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
    int start_column = column - (current - start);
    tokens.add(new Token(type, text, literal, line, start_column));
  }
}


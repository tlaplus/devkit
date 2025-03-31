package com.craftinginterpreters.tla;

import java.util.List;

import static com.craftinginterpreters.tla.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

  private Expr expression() {
    return prec3FixOp();
  }

  private Expr prec3FixOp() {
    Expr expr = prec4FixOp();

    while (match(AND, OR)) {
      Token operator = previous();
      Expr right = prec4FixOp();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr prec4FixOp() {
    while (match(NEGATION)) {
      Token operator = previous();
      Expr right = prec4FixOp();
      return new Expr.Unary(operator, right);
    }

    return prec5FixOp();
  }

  private Expr prec5FixOp() {
    Expr expr = prec9FixOp();

    while (match(EQUAL, LESS_THAN, IN)) {
      Token operator = previous();
      Expr right = prec9FixOp();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr prec9FixOp() {
    Expr expr = prec10FixOp();

    while (match(DOT_DOT)) {
      Token operator = previous();
      Expr right = prec10FixOp();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr prec10FixOp() {
    Expr expr = prec11FixOp();

    while (match(PLUS)) {
      Token operator = previous();
      Expr right = prec11FixOp();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr prec11FixOp() {
    Expr expr = prec12FixOp();

    while (match(MINUS)) {
      Token operator = previous();
      Expr right = prec12FixOp();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr prec12FixOp() {
    while (match(MINUS)) {
      Token operator = previous();
      Expr right = prec12FixOp();
      return new Expr.Unary(operator, right);
    }

    return prec15FixOp();
  }

  private Expr prec15FixOp() {
    while (match(ENABLED)) {
      Token operator = previous();
      Expr right = prec15FixOp();
      return new Expr.Unary(operator, right);
    }

    Expr expr = primary();
    while (match(PRIME)) {
      Token operator = previous();
      expr = new Expr.Unary(operator, expr);
    }

    return expr;
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);

    if (match(NAT_NUMBER)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    TlaPlus.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    return;
  }
}


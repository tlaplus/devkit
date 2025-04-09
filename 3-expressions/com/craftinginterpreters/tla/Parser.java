package com.craftinginterpreters.tla;

import java.util.List;
import java.util.ArrayList;

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
    if (match(IF)) {
      Expr condition = expression();
      consume(THEN, "'THEN' required after 'IF' expression.");
      Expr yes = expression();
      consume(ELSE, "'ELSE' required after 'THEN' expression.");
      Expr no = expression();
      return new Expr.ITE(condition, yes, no);
    }

    if (match(LEFT_BRACE)) {
      List<Expr> elements = new ArrayList<Expr>();
      if (match(RIGHT_BRACE)) {
        return new Expr.Set(elements);
      }
      do {
        elements.add(expression());
      } while (match(COMMA));
      consume(RIGHT_BRACE, "'}' is required to terminate finite set literal.");
      return new Expr.Set(elements);
    }

    if (match(LEFT_BRACKET)) {
      String intro = consume(IDENTIFIER, "Identifier required after '['.").lexeme;
      consume(IN, "'\\in' required after function constructor intro.");
      Expr set = expression();
      consume(ALL_MAP_TO, "'|->' is required after function constructor set.");
      Expr expr = expression();
      consume(RIGHT_BRACKET, "']' is required to terminate function constructor.");
      return new Expr.FnCons(intro, set, expr);
    }

    while (match(EXISTS, FOR_ALL)) {
      Token quantifier = previous();
      List<String> intros = new ArrayList<String>();
      do {
        Token intro = consume(IDENTIFIER, "Identifier is required after quantifier.");
        intros.add(intro.lexeme);
      } while (match(COMMA));
      consume(IN, "'\\in' is required after quantified identifier intro.");
      Expr set = expression();
      consume(COLON, "':' is required after quantification set expression.");
      Expr expr = expression();
      return new Expr.Quant(quantifier, intros, set, expr);
    }

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

    Expr expr = functionApplication();
    while (match(PRIME)) {
      Token operator = previous();
      expr = new Expr.Postfix(expr, operator);
    }

    return expr;
  }

  private Expr functionApplication() {
    Expr expr = primary();
    while (match(LEFT_BRACKET)) {
      Expr parameter = expression();
      consume(RIGHT_BRACKET, "Expect ']' after expression.");
      expr = new Expr.FnAppl(expr, parameter);
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


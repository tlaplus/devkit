package com.craftinginterpreters.tla;

import java.util.List;
import java.util.ArrayList;

import static com.craftinginterpreters.tla.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}
  
  enum Fix {
    PREFIX, INFIX, POSTFIX
  }
  static class Operator {
    final Fix fix;
    final TokenType token;
    final boolean assoc;
    final int lowPrec;
    final int highPrec;
    public Operator(Fix fix, TokenType token, boolean assoc,
                    int lowPrec, int highPrec) {
      this.fix = fix;
      this.token = token;
      this.assoc = assoc;
      this.lowPrec = lowPrec;
      this.highPrec = highPrec;
    }
  }
  
  private static final Operator[] operators = new Operator[] {
    new Operator(Fix.PREFIX,  NEGATION,   true,   4,  4 ),
    new Operator(Fix.PREFIX,  MINUS,      true,   12, 12),
    new Operator(Fix.PREFIX,  ENABLED,    false,  4,  15),
    new Operator(Fix.INFIX,   AND,        true,   3,  3 ),
    new Operator(Fix.INFIX,   OR,         true,   3,  3 ),
    new Operator(Fix.INFIX,   IN,         false,  5,  5 ),
    new Operator(Fix.INFIX,   EQUAL,      false,  5,  5 ),
    new Operator(Fix.INFIX,   LESS_THAN,  false,  5,  5 ),
    new Operator(Fix.INFIX,   DOT_DOT,    false,  9,  9 ),
    new Operator(Fix.INFIX,   PLUS,       true,   10, 10),
    new Operator(Fix.INFIX,   MINUS,      true,   11, 11),
    new Operator(Fix.POSTFIX, PRIME,      false,  15, 15),
  };
  
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
    return operatorExpression(1);
  }
  
  private Operator matchOp(int prec, Fix fix) {
    for (Operator op : operators) {
      if (op.lowPrec == prec && op.fix == fix && match(op.token)) {
        return op;
      }
    }

    return null;
  }
  
  private Expr operatorExpression(int prec) {
    if (prec == 16) return primary();

    Operator op;
    if ((op = matchOp(prec, Fix.PREFIX)) != null) {
      Token opToken = previous();
      Expr expr = operatorExpression(op.assoc ? op.lowPrec : op.highPrec + 1);
      return new Expr.Unary(opToken, expr);
    }

    Expr expr = operatorExpression(prec + 1);
    while ((op = matchOp(prec, Fix.INFIX)) != null) {
      Token operator = previous();
      Expr right = operatorExpression(prec + 1);
      expr = new Expr.Binary(expr, operator, right);
      if (!op.assoc) break;
    }
    
    while ((op = matchOp(prec, Fix.POSTFIX)) != null) {
      Token opToken = previous();
      expr = new Expr.Unary(opToken, expr);
      if (!op.assoc) break;
    }

    return expr;
  }
  
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);

    if (match(NUMBER)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if (match(IF)) {
      Token operator = previous();
      Expr condition = expression();
      consume(THEN, "'THEN' required after 'IF' expression.");
      Expr yes = expression();
      consume(ELSE, "'ELSE' required after 'THEN' expression.");
      Expr no = expression();
      return new Expr.Ternary(operator, condition, yes, no);
    }

    if (match(LEFT_BRACE)) {
      Token operator = previous();
      List<Expr> elements = new ArrayList<Expr>();
      if (RIGHT_BRACE != peek().type) {
        do {
          elements.add(expression());
        } while (match(COMMA));
      }
      consume(RIGHT_BRACE, "'}' is required to terminate finite set literal.");
      return new Expr.Variadic(operator, elements);
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


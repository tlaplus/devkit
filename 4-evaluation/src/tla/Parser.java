package tla;

import java.util.List;
import java.util.ArrayList;

import static tla.TokenType.*;
import static tla.Fix.*;

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
    return operatorExpression(1);
  }

  private Expr operatorExpression(int prec) {
    if (prec == 16) return primary();

    Operator op;
    if ((op = matchOp(PREFIX, prec)) != null) {
      Token opToken = previous();
      Expr expr = operatorExpression(op.assoc ? op.lowPrec : op.highPrec + 1);
      return new Expr.Unary(opToken, expr);
    }

    Expr expr = operatorExpression(prec + 1);
    while ((op = matchOp(INFIX, prec)) != null) {
      Token operator = previous();
      Expr right = operatorExpression(op.highPrec + 1);
      expr = new Expr.Binary(expr, operator, right);
      if (!op.assoc) return expr;
    }

    while ((op = matchOp(POSTFIX, prec)) != null) {
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

  private static final Operator[] operators = new Operator[] {
    new Operator(PREFIX,  NOT,        true,   4,  4 ),
    new Operator(PREFIX,  ENABLED,    false,  4,  15),
    new Operator(PREFIX,  MINUS,      true,   12, 12),
    new Operator(INFIX,   IN,         false,  5,  5 ),
    new Operator(INFIX,   EQUAL,      false,  5,  5 ),
    new Operator(INFIX,   LESS_THAN,  false,  5,  5 ),
    new Operator(INFIX,   DOT_DOT,    false,  9,  9 ),
    new Operator(INFIX,   PLUS,       true,   10, 10),
    new Operator(INFIX,   MINUS,      true,   11, 11),
    new Operator(POSTFIX, PRIME,      false,  15, 15),
  };

  private Operator matchOp(Fix fix, int prec) {
    for (Operator op : operators) {
      if (op.fix == fix && op.lowPrec == prec) {
        if (match(op.token)) return op;
      }
    }

    return null;
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
    advance();

    while(!isAtEnd()) {
      if (previous().type == EQUAL_EQUAL) return;

      advance();
    }
  }
}

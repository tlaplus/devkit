package tla;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;

import static tla.TokenType.*;

class Parser {
  private static enum Fix { PREFIX, INFIX, POSTFIX }
  private static record Operator(Fix fix, TokenType token,
      boolean assoc, int lowPrec, int highPrec) { }
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;
  private final boolean replMode;
  private final ArrayDeque<Integer> jlists = new ArrayDeque<>();

  Parser(List<Token> tokens, boolean replMode) {
    this.tokens = tokens;
    this.replMode = replMode;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Stmt declaration() {
    try {
      if (lookahead().isAtOpDefStart()) return operatorDefinition();

      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private boolean isAtOpDefStart() {
    if (!match(IDENTIFIER)) return false;

    return match(EQUAL_EQUAL);
  }

  private Stmt operatorDefinition() {
    Token name = consume(IDENTIFIER, "Name required for operator definition.");
    consume(EQUAL_EQUAL, "== required for operator definition.");
    return new Stmt.OpDef(name, expression());
  }

  private Stmt statement() {
    if (replMode) return new Stmt.Print(peek(), expression());

    throw error(peek(), "Expected statement.");
  }

  private Expr expression() {
    return operatorExpression(1);
  }

  private Expr operatorExpression(int prec) {
    if (prec == 16) return primary();

    Operator op;
    if ((op = matchOp(Fix.PREFIX, prec)) != null) {
      Token opToken = previous();
      Expr expr = operatorExpression(op.assoc ? op.lowPrec : op.highPrec + 1);
      return new Expr.Unary(opToken, expr);
    }

    Expr expr = operatorExpression(prec + 1);
    while ((op = matchOp(Fix.INFIX, prec)) != null) {
      Token operator = previous();
      Expr right = operatorExpression(op.highPrec + 1);
      expr = flattenInfix(expr, operator, right);
      if (!op.assoc) return expr;
    }

    while ((op = matchOp(Fix.POSTFIX, prec)) != null) {
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

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
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
      List<Expr> elements = new ArrayList<>();
      if (!check(RIGHT_BRACE)) {
        do {
          elements.add(expression());
        } while (match(COMMA));
      }
      consume(RIGHT_BRACE, "'}' is required to terminate finite set literal.");
      return new Expr.Variadic(operator, elements);
    }

    if (match(AND, OR)) {
      Token op = previous();
      jlists.push(op.column);
      List<Expr> juncts = new ArrayList<>();
      do {
        juncts.add(expression());
      } while (matchBullet(op.type, op.column));
      jlists.pop();
      return flattenJLists(op, juncts);
    }

    throw error(peek(), "Expect expression.");
  }

  private static final Operator[] operators = new Operator[] {
    new Operator(Fix.PREFIX,  NOT,        true,   4,  4 ),
    new Operator(Fix.PREFIX,  ENABLED,    false,  4,  15),
    new Operator(Fix.PREFIX,  MINUS,      true,   12, 12),
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

  private Expr flattenInfix(Expr left, Token op, Expr right) {
    if (op.type == AND) {
      List<Expr> conjuncts = new ArrayList<>();
      conjuncts.add(left);
      conjuncts.add(right);
      return flattenJLists(op, conjuncts);
    } else if (op.type == OR) {
      List<Expr> disjuncts = new ArrayList<>();
      disjuncts.add(left);
      disjuncts.add(right);
      return flattenJLists(op, disjuncts);
    } else {
      return new Expr.Binary(left, op, right);
    }
  }

  private Expr flattenJLists(Token op, List<Expr> juncts) {
    List<Expr> flattened = new ArrayList<>();
    for (Expr junct : juncts) {
      Expr.Variadic vjunct;
      if ((vjunct = asVariadicOp(op, junct)) != null) {
        flattened.addAll(vjunct.parameters);
      } else {
        flattened.add(junct);
      }
    }

    return new Expr.Variadic(op, flattened);
  }

  private Expr.Variadic asVariadicOp(Token op, Expr expr) {
    if (expr instanceof Expr.Variadic) {
      Expr.Variadic vExpr = (Expr.Variadic)expr;
      if (vExpr.operator.type == op.type) return vExpr;
    }

    return null;
  }

  private Parser lookahead() {
    Parser lookahead = new Parser(tokens, replMode);
    lookahead.current = current;
    return lookahead;
  }

  private boolean matchBullet(TokenType op, int column) {
    if (peek().type == op && peek().column == column) {
      advance();
      return true;
    }

    return false;
  }

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
    if (!jlists.isEmpty() && peek().column <= jlists.peek()) return false;
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
    jlists.clear();
    advance();

    while(!isAtEnd()) {
      if (lookahead().isAtOpDefStart()) return;

      advance();
    }
  }
}

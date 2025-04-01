package com.craftinginterpreters.tla;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }
  
  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitPostfixExpr(Expr.Postfix expr) {
    return parenthesize(expr.operator.lexeme, expr.left);
  }

  @Override
  public String visitITEExpr(Expr.ITE expr) {
    return parenthesize("ITE", expr.condition, expr.yes, expr.no);
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize("Set", expr.elements.toArray(Expr[]::new));
  }

  @Override
  public String visitFnConsExpr(Expr.FnCons expr) {
    return parenthesize("FnCons", expr.set, expr.expr);
  }

  @Override
  public String visitFnApplExpr(Expr.FnAppl expr) {
    return parenthesize("FnAppl", expr.name, expr.parameter);
  }

  @Override
  public String visitQuantExpr(Expr.Quant expr) {
    return parenthesize(expr.quantifier.lexeme, expr.set, expr.expr);
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1, 0),
            new Expr.Literal(123)),
        new Token(TokenType.PLUS, "+", null, 1, 0),
        new Expr.Grouping(
            new Expr.Literal(45)));

    System.out.println(new AstPrinter().print(expression));
  }
}


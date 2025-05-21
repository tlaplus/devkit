package tla;

import java.util.List;
import java.util.stream.Collectors;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  String print(List<Stmt> statements) {
    return statements
        .stream()
        .map(statement -> statement.accept(this))
        .collect(Collectors.joining(" "));
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
  public String visitQuantFnExpr(Expr.QuantFn expr) {
    String params = " " + expr.params.stream().map(t -> t.lexeme).collect(Collectors.joining(" "));
    return parenthesize(expr.op.lexeme + params, expr.set, expr.body);
  }

  @Override
  public String visitFnApplyExpr(Expr.FnApply expr) {
    return parenthesize(expr.fn.accept(this), expr.argument);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return parenthesize(expr.name.lexeme,
                        expr.arguments.toArray(Expr[]::new));
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.expr);
  }

  @Override
  public String visitTernaryExpr(Expr.Ternary expr) {
    return parenthesize(expr.operator.lexeme, expr.first,
                        expr.second, expr.third);
  }

  @Override
  public String visitVariadicExpr(Expr.Variadic expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.parameters.toArray(Expr[]::new));
  }

  @Override
  public String visitOpDefStmt(Stmt.OpDef stmt) {
    String params =
        stmt.params.isEmpty() ? "" : " " +
        stmt.params.stream().map(t -> t.lexeme).collect(Collectors.joining(" "));
    return parenthesize(
        stmt.name.lexeme + params,
        stmt.body
    );
  }

  @Override
  public String visitVarDeclStmt(Stmt.VarDecl stmt) {
    return parenthesize(
        "VARIABLE " +
        stmt.names.stream()
            .map(t -> t.lexeme)
            .collect(Collectors.joining(" ")));
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
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
}

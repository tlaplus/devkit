package tla;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitQuantFnExpr(QuantFn expr);
    R visitFnApplyExpr(FnApply expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitVariableExpr(Variable expr);
    R visitUnaryExpr(Unary expr);
    R visitTernaryExpr(Ternary expr);
    R visitVariadicExpr(Variadic expr);
  }
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class QuantFn extends Expr {
    QuantFn(Token op, List<Token> params, Expr set, Expr body) {
      this.op = op;
      this.params = params;
      this.set = set;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitQuantFnExpr(this);
    }

    final Token op;
    final List<Token> params;
    final Expr set;
    final Expr body;
  }
  static class FnApply extends Expr {
    FnApply(Expr fn, Token bracket, Expr argument) {
      this.fn = fn;
      this.bracket = bracket;
      this.argument = argument;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFnApplyExpr(this);
    }

    final Expr fn;
    final Token bracket;
    final Expr argument;
  }
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
  static class Variable extends Expr {
    Variable(Token name, List<Expr> arguments) {
      this.name = name;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
    final List<Expr> arguments;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr expr) {
      this.operator = operator;
      this.expr = expr;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr expr;
  }
  static class Ternary extends Expr {
    Ternary(Token operator, Expr first, Expr second, Expr third) {
      this.operator = operator;
      this.first = first;
      this.second = second;
      this.third = third;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTernaryExpr(this);
    }

    final Token operator;
    final Expr first;
    final Expr second;
    final Expr third;
  }
  static class Variadic extends Expr {
    Variadic(Token operator, List<Expr> parameters) {
      this.operator = operator;
      this.parameters = parameters;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariadicExpr(this);
    }

    final Token operator;
    final List<Expr> parameters;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

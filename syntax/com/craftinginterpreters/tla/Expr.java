package com.craftinginterpreters.tla;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
    R visitFnApplExpr(FnAppl expr);
    R visitFnConsExpr(FnCons expr);
    R visitQuantExpr(Quant expr);
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
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
  static class FnAppl extends Expr {
    FnAppl(Expr name, Expr parameter) {
      this.name = name;
      this.parameter = parameter;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFnApplExpr(this);
    }

    final Expr name;
    final Expr parameter;
  }
  static class FnCons extends Expr {
    FnCons(String intro, Expr set, Expr expr) {
      this.intro = intro;
      this.set = set;
      this.expr = expr;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFnConsExpr(this);
    }

    final String intro;
    final Expr set;
    final Expr expr;
  }
  static class Quant extends Expr {
    Quant(TokenType quantifier, List<String> intros, Expr set, Expr expr) {
      this.quantifier = quantifier;
      this.intros = intros;
      this.set = set;
      this.expr = expr;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitQuantExpr(this);
    }

    final TokenType quantifier;
    final List<String> intros;
    final Expr set;
    final Expr expr;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

package tla;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitPrintStmt(Print stmt);
    R visitOpDefStmt(OpDef stmt);
  }
  static class Print extends Stmt {
    Print(Token location, Expr expression) {
      this.location = location;
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Token location;
    final Expr expression;
  }
  static class OpDef extends Stmt {
    OpDef(Token name, Expr body) {
      this.name = name;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitOpDefStmt(this);
    }

    final Token name;
    final Expr body;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

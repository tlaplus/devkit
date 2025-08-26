package tla;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitVarDeclStmt(VarDecl stmt);
    R visitPrintStmt(Print stmt);
    R visitOpDefStmt(OpDef stmt);
  }
  static class VarDecl extends Stmt {
    VarDecl(List<Token> names) {
      this.names = names;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarDeclStmt(this);
    }

    final List<Token> names;
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
    OpDef(Token name, List<Token> params, Expr body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitOpDefStmt(this);
    }

    final Token name;
    final List<Token> params;
    final Expr body;
  }

  abstract <R> R accept(Visitor<R> visitor);
}

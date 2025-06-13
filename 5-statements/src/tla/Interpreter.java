package tla;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
  private Environment environment;

  public Interpreter(boolean replMode) {
    this.environment = new Environment(replMode);
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      TlaPlus.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitOpDefStmt(Stmt.OpDef stmt) {
    Object value = evaluate(stmt.body);
    environment.define(stmt.name, value);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case DOT_DOT:
        checkNumberOperands(expr.operator, left, right);
        Set<Object> set = new HashSet<Object>();
        int lower = (int)left;
        int higher = (int)right;
        if (lower <= higher) {
          for (int i = lower; i <= higher; i++) {
            set.add(i);
          }
        }
        return set;
      case IN:
        checkSetOperand(expr.operator, right);
        return ((Set<?>)right).contains(left);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (int)left - (int)right;
      case PLUS:
        checkNumberOperands(expr.operator, left, right);
        return (int)left + (int)right;
      case LESS_THAN:
        checkNumberOperands(expr.operator, left, right);
        return (int)left < (int)right;
      case EQUAL:
        return left.equals(right);
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object operand = evaluate(expr.expr);

    switch (expr.operator.type) {
      case PRIME:
        return operand;
      case ENABLED:
        checkBooleanOperand(expr.operator, operand);
        return (boolean)operand;
      case NOT:
        checkBooleanOperand(expr.operator, operand);
        return !(boolean)operand;
      case MINUS:
        checkNumberOperand(expr.operator, operand);
        return -(int)operand;
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitTernaryExpr(Expr.Ternary expr) {
    switch (expr.operator.type) {
      case IF:
        Object conditional = evaluate(expr.first);
        checkBooleanOperand(expr.operator, conditional);
        return (boolean)conditional ?
            evaluate(expr.second) : evaluate(expr.third);
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitVariadicExpr(Expr.Variadic expr) {
    switch (expr.operator.type) {
      case LEFT_BRACE:
        Set<Object> set = new HashSet<Object>();
        for (Expr parameter : expr.parameters) {
          set.add(evaluate(parameter));
        }
        return set;
      default:
        // Unreachable.
        return null;
    }
  }

  private String stringify(Object object) {
    return object.toString();
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Integer) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator,
      Object left, Object right) {
    if (left instanceof Integer && right instanceof Integer) return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private void checkBooleanOperand(Token operator, Object operand) {
    if (operand instanceof Boolean) return;
    throw new RuntimeError(operator, "Operand must be a boolean.");
  }

  private void checkSetOperand(Token operator, Object operand) {
    if (operand instanceof Set<?>) return;
    throw new RuntimeError(operator, "Operand must be a set.");
  }
}

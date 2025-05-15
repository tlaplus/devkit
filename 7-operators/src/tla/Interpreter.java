package tla;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.PrintStream;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
  final Environment globals;
  private Environment environment;
  private final PrintStream out;

  public Interpreter(PrintStream out, boolean replMode) {
    this.globals = new Environment(replMode);
    this.environment = this.globals;
    this.out = out;
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

  Object execute(Expr expr, Environment environment) {
    Environment old = this.environment;
    this.environment = environment;
    try {
      Object result = evaluate(expr);
      return result;
    } finally {
      this.environment = old;
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitOpDefStmt(Stmt.OpDef stmt) {
    TlaOperator op = new TlaOperator(stmt);
    environment.define(stmt.name, op);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    out.println(stringify(value));
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
  public Object visitQuantFnExpr(Expr.QuantFn expr) {
    Object set = evaluate(expr.set);
    checkSetOperand(expr.op, set);
    VarBinder bindings = new VarBinder(expr.params, (Set<?>)set, environment);
    switch (expr.op.type) {
      case ALL_MAP_TO: {
        Token param = expr.params.get(0);
        Map<Object, Object> function = new HashMap<>();
        for (Environment binding : bindings) {
          function.put(binding.get(param), execute(expr.body, binding));
        }
        return function;
      } case FOR_ALL: {
        for (Environment binding : bindings) {
          Object result = execute(expr.body, binding);
          checkBooleanOperand(expr.op, result);
          if (!(Boolean)result) return false;
        }
        return true;
      } case EXISTS: {
        boolean result = false;
        for (Environment binding : bindings) {
          Object junctResult = execute(expr.body, binding);
          checkBooleanOperand(expr.op, junctResult);
          result |= (Boolean)junctResult;
        }
        return result;
      } default: {
        // Unreachable.
        return null;
      }
    }
  }

  @Override
  public Object visitFnApplyExpr(Expr.FnApply expr) {
    Object function = evaluate(expr.fn);
    checkFunctionOperand(expr.bracket, function);
    Object argument = evaluate(expr.argument);
    Map<?, ?> map = (Map<?, ?>)function;
    if (!map.containsKey(argument)) {
      throw new RuntimeError(expr.bracket,
          "Cannot apply function to element outside domain: "
          + argument.toString());
    }

    return map.get(argument);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object callee = environment.get(expr.name);

    /*
    if (!(callee instanceof TlaCallable)) {
      if (!expr.arguments.isEmpty()) {
        throw new RuntimeError(expr.name,
            "Cannot give arguments to non-operator identifier.");
      }

      return callee;
    }*/

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(new TlaOperator(argument));
    }

    TlaCallable operator = (TlaCallable)callee;
    /*
    if (arguments.size() != operator.arity()) {
      throw new RuntimeError(expr.name, "Expected " +
          operator.arity() + " arguments but got " +
          arguments.size() + ".");
    }*/

    return operator.call(this, arguments);
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
      case AND:
        for (Expr conjunct : expr.parameters) {
          Object result = evaluate(conjunct);
          checkBooleanOperand(expr.operator, result);
          if (!(boolean)result) return false;
        }
        return true;
      case OR:
        for (Expr disjunct : expr.parameters) {
          Object result = evaluate(disjunct);
          checkBooleanOperand(expr.operator, result);
          if ((boolean)result) return true;
        }
        return false;
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

  private void checkFunctionOperand(Token operator, Object operand) {
    if (operand instanceof Map<?,?>) return;
    throw new RuntimeError(operator, "Operand must be a function.");
  }
}

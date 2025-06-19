package tla;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
  final Environment globals;
  private Environment environment;

  public Interpreter(boolean replMode) {
    this.globals = new Environment(replMode);
    this.environment = this.globals;
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

  Object executeBlock(Expr expr, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      return evaluate(expr);
    } finally {
      this.environment = previous;
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitOpDefStmt(Stmt.OpDef stmt) {
    checkNotDefined(stmt.params);
    for (Token param : stmt.params) {
      if (param.lexeme.equals(stmt.name.lexeme)) {
        throw new RuntimeError(param, "Identifier already in use.");
      }
    }

    TlaOperator op = new TlaOperator(stmt);
    environment.define(stmt.name, op);
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
  public Object visitQuantFnExpr(Expr.QuantFn expr) {
    checkNotDefined(expr.params);
    Object set = evaluate(expr.set);
    checkSetOperand(expr.op, set);
    BindingGenerator bindings = new BindingGenerator(expr.params, (Set<?>)set, environment);
    switch (expr.op.type) {
      case ALL_MAP_TO: {
        Token param = expr.params.get(0);
        Map<Object, Object> function = new HashMap<>();
        for (Environment binding : bindings) {
          Object value = executeBlock(expr.body, binding);
          function.put(binding.get(param), value);
        }
        return function;
      } case FOR_ALL: {
        for (Environment binding : bindings) {
          Object result = executeBlock(expr.body, binding);
          checkBooleanOperand(expr.op, result);
          if (!(Boolean)result) return false;
        }
        return true;
      } case EXISTS: {
        boolean result = false;
        for (Environment binding : bindings) {
          Object junctResult = executeBlock(expr.body, binding);
          checkBooleanOperand(expr.op, junctResult);
          result |= (boolean)junctResult;
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
    Object callee = evaluate(expr.fn);
    checkFunctionOperand(expr.bracket, callee);
    Object argument = evaluate(expr.argument);
    Map<?, ?> function = (Map<?, ?>)callee;
    if (!function.containsKey(argument)) {
      throw new RuntimeError(expr.bracket,
          "Cannot apply function to element outside domain: "
          + argument.toString());
    }

    return function.get(argument);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object callee = environment.get(expr.name);

    if (!(callee instanceof TlaCallable)) {
      if (!expr.arguments.isEmpty()) {
        throw new RuntimeError(expr.name,
            "Cannot give arguments to non-operator identifier.");
      }

      return callee;
    }

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    TlaCallable operator = (TlaCallable)callee;
    if (arguments.size() != operator.arity()) {
      throw new RuntimeError(expr.name, "Expected " +
          operator.arity() + " arguments but got " +
          arguments.size() + ".");
    }

    return operator.call(this, arguments);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    switch (expr.operator.type) {
      case PRIME: {
        return evaluate(expr.expr);
      } case ENABLED: {
        return false;
      } case NOT: {
        Object operand = evaluate(expr.expr);
        checkBooleanOperand(expr.operator, operand);
        return !(boolean)operand;
      } case MINUS: {
        Object operand = evaluate(expr.expr);
        checkNumberOperand(expr.operator, operand);
        return -(int)operand;
      } default: {
        // Unreachable.
        return null;
      }
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
        boolean result = false;
        for (Expr disjunct : expr.parameters) {
          Object junctResult = evaluate(disjunct);
          checkBooleanOperand(expr.operator, junctResult);
          result |= (boolean)junctResult;
        }
        return result;
      default:
        // Unreachable.
        return null;
    }
  }

  private String stringify(Object object) {
    return object.toString();
  }

  private void checkNotDefined(List<Token> names) {
    for (Token name : names) {
      if (environment.isDefined(name)) {
        throw new RuntimeError(name, "Identifier already in use.");
      }
    }

    for (int i = 0; i < names.size() - 1; i++) {
      for (int j = i + 1; j < names.size(); j++) {
        if (names.get(i).lexeme.equals(names.get(j).lexeme)) {
          throw new RuntimeError(names.get(i), "Identifier used twice in same list.");
        }
      }
    }
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

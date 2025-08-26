package tla;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {

  private record UnboundVariable(Token name) {
    @Override
    public String toString() {
      return name.lexeme;
    }
  }

  final Environment globals;
  private Environment environment;

  private final Map<String, Token> variables = new HashMap<>();
  private Map<String, Object> current = null;
  private Map<String, Object> next = new HashMap<>();
  private Set<Map<String, Object>> possibleNext = new HashSet<>();
  private boolean primed = true;

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

  List<Map<String, Object>> getNextStates(Token location, Expr action) {
    Set<Map<String, Object>> confirmedNext = new HashSet<>();
    clearNext();
    possibleNext.add(new HashMap<>(next));
    try {
      while (!possibleNext.isEmpty()) {
        Map<String, Object> trunk = possibleNext.iterator().next();
        next = new HashMap<>(trunk);
        Object satisfied = evaluate(action);
        checkBooleanOperand(location, satisfied);
        if ((boolean)satisfied && isComplete()) confirmedNext.add(next);
        possibleNext.remove(trunk);
      }
    } finally {
      clearNext();
    }

    return new ArrayList<>(confirmedNext);
  }

  void goToState(Map<String, Object> state) {
    current = state;
    primed = state == null;
    clearNext();
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

  private boolean isComplete() {
    return !variables.isEmpty() && next.values().stream()
        .noneMatch(v -> v instanceof UnboundVariable);
  }

  private void clearNext() {
    possibleNext = new HashSet<>();
    next = new HashMap<>();
    for (Token variable : variables.values()) {
      next.put(variable.lexeme, new UnboundVariable(variable));
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

    if (variables.containsKey(stmt.name.lexeme)) {
      throw new RuntimeError(stmt.name, "State variable redeclared as operator.");
    }

    TlaOperator op = new TlaOperator(stmt);
    environment.define(stmt.name, op);
    return null;
  }

  @Override
  public Void visitVarDeclStmt(Stmt.VarDecl stmt) {
    checkNotDefined(stmt.names);
    for (Token name : stmt.names) {
      variables.put(name.lexeme, name);
      next.put(name.lexeme, new UnboundVariable(name));
    }

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
        if (left instanceof UnboundVariable) {
          UnboundVariable var = (UnboundVariable)left;
          Map<String, Object> trunk = next;
          for (Object element : (Set<?>)right) {
            next = new HashMap<>(trunk);
            next.put(var.name().lexeme, element);
            left = element;
            possibleNext.add(new HashMap<>(next));
          }
          return true;
        }
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
        if (left instanceof UnboundVariable) {
          UnboundVariable var = (UnboundVariable)left;
          checkIsValue(right);
          next.put(var.name().lexeme, right);
          return true;
        }
        checkIsValue(left, right);
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
          checkIsValue(value);
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
        Map<String, Object> trunk = next;
        for (Environment binding : bindings) {
          next = new HashMap<>(trunk);
          Object junctResult = executeBlock(expr.body, binding);
          checkBooleanOperand(expr.op, junctResult);
          possibleNext.add(new HashMap<>(next));
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
    checkIsValue(argument);
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
    Object callee =
      variables.containsKey(expr.name.lexeme)
      ? (primed ? next : current).get(expr.name.lexeme)
      : environment.get(expr.name);

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
        if (primed) {
          throw new RuntimeError(expr.operator,
              "Cannot double-prime expression nor prime initial state.");
        } try {
          primed = true;
          return evaluate(expr.expr);
        } finally {
          primed = false;
        }
      } case ENABLED: {
        Map<String, Object> oldNext = next;
        Set<Map<String,Object>> oldPossibleNext = possibleNext;
        try {
          clearNext();
          return !getNextStates(expr.operator, expr.expr).isEmpty();
        } finally {
          next = oldNext;
          possibleNext = oldPossibleNext;
        }
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
          Object value = evaluate(parameter);
          checkIsValue(value);
          set.add(value);
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
        Map<String, Object> trunk = next;
        for (Expr disjunct : expr.parameters) {
          next = new HashMap<>(trunk);
          Object junctResult = evaluate(disjunct);
          checkBooleanOperand(expr.operator, junctResult);
          possibleNext.add(new HashMap<>(next));
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

  private void checkIsValue(Object... operands) {
    for (Object operand : operands) {
      if (operand instanceof UnboundVariable) {
        UnboundVariable var = (UnboundVariable)operand;
        throw new RuntimeError(var.name(), "Use of unbound variable.");
      }
    }
  }

  private void checkNotDefined(List<Token> names) {
    for (Token name : names) {
      if (environment.isDefined(name)) {
        throw new RuntimeError(name, "Identifier already in use.");
      }

      if (variables.containsKey(name.lexeme)) {
        throw new RuntimeError(name, "Name conflicts with state variable.");
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

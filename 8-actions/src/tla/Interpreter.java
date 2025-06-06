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
  private State state = new State();
  private final Set<State> possibleNext = new HashSet<>();

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

  List<State> getNextStates(Stmt.OpDef action, State current) {
    if (!action.params.isEmpty()) {
      throw new RuntimeError(action.name,
          "Cannot use parameterized operator as top-level action.");
    }

    Set<State> nextStates = new HashSet<>();
    try {
      possibleNext.add(current);
      while (!possibleNext.isEmpty()) {
        state = possibleNext.iterator().next();
        Object satisfies = evaluate(action.body);
        checkBooleanOperand(action.name, satisfies);
        if ((boolean)satisfies && state.isCompletelyDefined()) nextStates.add(state);
        possibleNext.remove(state);
      }
    } finally {
      possibleNext.clear();
      state.reset();
    }

    return new ArrayList<>(nextStates);
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

    if (state.isDeclared(stmt.name)) {
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
      state.declareVariable(name);
    }

    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    try {
      Object value = evaluate(stmt.expression);
      if (!(value instanceof Boolean) || !state.isCompletelyDefined()) {
        out.println(stringify(value));
        return null;
      }
    } finally {
      state.reset();
    }

    Stmt.OpDef action = new Stmt.OpDef(stmt.location, new ArrayList<>(), stmt.expression);
    List<State> nextStates = getNextStates(action, state);

    if (nextStates.size() == 0) {
      out.println(stringify(false));
    } else if (nextStates.size() == 1) {
      out.println(stringify(true));
      out.println(state.toString());
      state = nextStates.get(0);
      state.step();
    } else {
      out.println(stringify(true));
      out.println("Select " + (state.isInitialState() ? "initial" : "next") + " state (number):");
      for (int i = 0; i < nextStates.size(); i++) {
        out.println(i + ":");
        out.println(nextStates.get(i));
      }
      out.print("> ");
      try (java.util.Scanner in = new java.util.Scanner(System.in)) {
        int selection = in.nextInt();
        state = nextStates.get(selection);
      }
      out.println(stringify(state.toString()));
      state.step();
    }

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
          if (var.primed() || state.isInitialState()) {
            State current = state;
            for (Object element : (Set<?>)right) {
              state = new State(current);
              state.bindValue(var, element);
              left = element;
              possibleNext.add(state);
            }
          }
        }
        checkIsDefined(left);
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
          checkIsDefined(right);
          state.bindValue(var, right);
          return true;
        }
        checkIsDefined(left, right);
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
          function.put(binding.get(param), executeBlock(expr.body, binding));
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
        State current = state;
        for (Environment binding : bindings) {
          state = new State(current);
          Object junctResult = executeBlock(expr.body, binding);
          checkBooleanOperand(expr.op, junctResult);
          possibleNext.add(state);
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
    Object function = evaluate(expr.fn);
    checkFunctionOperand(expr.paren, function);
    Object argument = evaluate(expr.argument);
    Map<?, ?> map = (Map<?, ?>)function;
    if (!map.containsKey(argument)) {
      throw new RuntimeError(expr.paren,
          "Cannot apply function to element outside domain: "
          + argument.toString());
    }

    return map.get(argument);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object referent = getValue(expr.name);

    if (referent instanceof UnboundVariable) {
      return referent;
    }

    if (!(referent instanceof TlaCallable)) {
      if (!expr.arguments.isEmpty()) {
        throw new RuntimeError(expr.name,
            "Cannot call non-operator identifier.");
      }

      return referent;
    }

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    TlaCallable operator = (TlaCallable)referent;
    if (arguments.size() != operator.arity()) {
      throw new RuntimeError(expr.name, "Expected " +
          operator.arity() + " arguments but got " +
          arguments.size() + ".");
    }

    return operator.call(this, arguments);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    if (TokenType.PRIME == expr.operator.type) {
      state.prime(expr.operator);
      try {
        return evaluate(expr.expr);
      } finally {
        state.unprime();
      }
    }

    Object operand = evaluate(expr.expr);
    switch (expr.operator.type) {
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
          Object value = evaluate(parameter);
          checkIsDefined(value);
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
        State current = state;
        for (Expr disjunct : expr.parameters) {
          state = new State(current);
          Object junctResult = evaluate(disjunct);
          checkBooleanOperand(expr.operator, junctResult);
          possibleNext.add(state);
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

  private Object getValue(Token name) {
    if (state.isDeclared(name)) {
      return state.getValue(name);
    }

    return environment.get(name);
  }

  private void checkIsDefined(Object... operands) {
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

      if (state.isDeclared(name)) {
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

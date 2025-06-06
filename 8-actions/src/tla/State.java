package tla;

import java.util.HashMap;
import java.util.Map;

class State {
  private boolean initialState = true;
  private boolean primed = false;
  private Map<String, Token> variables = new HashMap<>();
  private Map<String, Object> current = new HashMap<>();
  private Map<String, Object> next = new HashMap<>();

  State() { }

  State(State other) {
    this.variables = new HashMap<>(other.variables);
    this.current = new HashMap<>(other.current);
    this.next = new HashMap<>(other.next);
    this.initialState = other.initialState;
    this.primed = other.primed;
  }

  boolean isDeclared(Token name) {
    return variables.containsKey(name.lexeme);
  }

  void declareVariable(Token name) {
    if (isDeclared(name)) {
      throw new RuntimeError(name, "Redeclared state variable.");
    }

    variables.put(name.lexeme, name);
    current.put(name.lexeme, new UnboundVariable(name, false));
    next.put(name.lexeme, new UnboundVariable(name, true));
  }

  void bindValue(UnboundVariable var, Object value) {
    if (var.primed() && initialState) {
      throw new RuntimeError(var.name(),
          "Cannot prime variable in initial state.");
    }

    (var.primed() ? next : current).put(var.name().lexeme, value);
  }

  Object getValue(Token name) {
    return (primed ? next : current).get(name.lexeme);
  }

  boolean isCompletelyDefined() {
    Map<String, Object> binding = initialState ? current : next;
    return !binding.isEmpty() && binding.values().stream()
        .noneMatch(v -> v instanceof UnboundVariable);
  }

  void reset() {
    Map<String, Object> binding = initialState ? current : next;
    for (Map.Entry<String, Token> var : variables.entrySet()) {
      binding.put(var.getKey(), new UnboundVariable(var.getValue(), !initialState));
    }
  }

  void prime(Token op) {
    if (primed) {
      throw new RuntimeError(op, "Cannot double-prime expression.");
    }

    primed = true;
  }

  void unprime() {
    primed = false;
  }

  boolean initialState() {
    return initialState;
  }

  void step() {
    if (initialState) {
      initialState = false;
    } else {
      current = next;
      next = new HashMap<>();
      for (Map.Entry<String, Token> var : variables.entrySet()) {
        next.put(var.getKey(), new UnboundVariable(var.getValue(), true));
      }
    }
  }

  @Override
  public String toString() {
    return initialState ?
        current.toString() :
        "Current: " + current.toString() + "\n" +
        "Next:    " + next.toString();
  }
}

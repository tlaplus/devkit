package tla;

import java.util.HashMap;
import java.util.Map;

class State {
  private Map<String, Token> variables = new HashMap<>();
  private Map<String, Object> currentState = new HashMap<>();
  private Map<String, Object> nextState = new HashMap<>();
  private boolean isInitialState = true;
  private boolean isPrimed = false;

  State() { }

  State(State other) {
    this.variables = new HashMap<>(other.variables);
    this.currentState = new HashMap<>(other.currentState);
    this.nextState = new HashMap<>(other.nextState);
    this.isInitialState = other.isInitialState;
    this.isPrimed = other.isPrimed;
  }

  void declareVariable(Token name) {
    if (variables.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "Redeclared state variable.");
    }

    variables.put(name.lexeme, name);
    UnboundVariable var = new UnboundVariable(name, false);
    currentState.put(name.lexeme, var);
    nextState.put(name.lexeme, var.prime());
  }

  boolean isDeclared(Token name) {
    return variables.containsKey(name.lexeme);
  }

  void bindValue(UnboundVariable var, Object value) {
    if (var.isPrimed() && isInitialState) {
      throw new RuntimeError(var.name(),
          "Cannot prime variable in initial state.");
    }

    Map<String, Object> state = state(var.isPrimed());
    state.put(var.name().lexeme, value);
  }

  Object getValue(Token name) {
    return state(isPrimed).get(name.lexeme);
  }

  boolean isCompletelyDefined() {
    Map<String, Object> binding = isInitialState ? currentState : nextState;
    return !binding.isEmpty() && binding.values().stream()
        .noneMatch(v -> v instanceof UnboundVariable);
  }

  void reset() {
    Map<String, Object> binding = isInitialState ? currentState : nextState;
    for (Map.Entry<String, Token> var : variables.entrySet()) {
      binding.put(var.getKey(), new UnboundVariable(var.getValue(), !isInitialState));
    }
  }

  void prime(Token op) {
    if (isPrimed) {
      throw new RuntimeError(op,
          "Cannot double-prime an expression.");
    }
    isPrimed = true;
  }

  void unPrime() {
    isPrimed = false;
  }

  boolean isPrimed() {
    return isPrimed;
  }

  boolean isInitialState() {
    return isInitialState;
  }

  void step() {
    if (isInitialState) {
      isInitialState = false;
    } else {
      currentState = nextState;
      nextState = new HashMap<>();
      for (Map.Entry<String, Token> var : variables.entrySet()) {
        nextState.put(var.getKey(), new UnboundVariable(var.getValue(), true));
      }
    }
  }

  private Map<String, Object> state(boolean isPrimed) {
    return isPrimed ? nextState : currentState;
  }

  @Override
  public String toString() {
    return isInitialState ?
        currentState.toString() :
        "Current: " + currentState.toString() + "\n" +
        "Next:    " + nextState.toString();
  }
}

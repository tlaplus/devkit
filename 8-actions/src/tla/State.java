package tla;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class State {
  private Set<Token> variables = new HashSet<>();
  private Map<String, Object> currentState = new HashMap<>();
  private Map<String, Object> nextState = new HashMap<>();
  private boolean isPrimed = false;

  void declareVariable(Token name) {
    if (variables.contains(name)) {
      throw new RuntimeError(name,
          "Re-declared state variable.");
    }

    variables.add(name);
    UnboundVariable var = new UnboundVariable(name, false);
    currentState.put(name.lexeme, var);
    nextState.put(name.lexeme, var.prime());
  }

  boolean isDeclared(Token name) {
    return variables.contains(name);
  }

  void bindValue(UnboundVariable var, Object value) {
    if (var.isPrimed() && isInitialState()) {
      throw new RuntimeError(var.name(),
          "Cannot prime variable in initial state.");
    }

    Map<String, Object> state = state(var.isPrimed());
    state.put(var.name().lexeme, value);
  }
  
  Object getValue(Token name) {
    return state(isPrimed).get(name.lexeme);
  }
 
  boolean isInitialState() {
    return anyUnbound(currentState);
  }
  
  boolean anyUnbound(Map<String, Object> binding) {
    return binding
        .values()
        .stream()
        .anyMatch(v -> v instanceof UnboundVariable);
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
  
  void step() {
    currentState = nextState;
    nextState = new HashMap<>();
  }

  private Map<String, Object> state(boolean isPrimed) {
    return isPrimed ? nextState : currentState;
  }
}

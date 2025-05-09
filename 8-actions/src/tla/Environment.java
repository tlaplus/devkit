package tla;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing;
  private final boolean allowRedefinition;
  private final Map<String, Object> values = new HashMap<>();
  private final State state;

  Environment(boolean allowRedefinition, State state) {
    enclosing = null;
    this.allowRedefinition = allowRedefinition;
    this.state = state;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
    this.allowRedefinition = enclosing.allowRedefinition;
    this.state = enclosing.state;
  }

  void define(Token name, Object value) {
    if (!allowRedefinition && values.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "Redefined definition '" + name.lexeme + "'.");
    }

    values.put(name.lexeme, value);
  }
  
  Object get(Token name) {
    if (state.isDeclared(name)) {
      return state.getValue(name);
    }

    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    
    if (enclosing != null) return enclosing.get(name);

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
}

package tla;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final boolean allowRedefinition;
  private final Map<String, Object> values = new HashMap<>();
  
  public Environment(boolean allowRedefinition) {
    this.allowRedefinition = allowRedefinition;
  }

  void define(Token name, Object value) {
    if (!allowRedefinition && values.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "Redefined definition '" + name.lexeme + "'.");
    }

    values.put(name.lexeme, value);
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
}

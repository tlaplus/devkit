package tla;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ModelChecker {

  private final Interpreter interpreter;
  private Stmt.OpDef init = null;
  private Stmt.OpDef next = null;
  private Stmt.OpDef invariantDef = null;

  ModelChecker(Interpreter interpreter, List<Stmt> spec) {
    this.interpreter = interpreter;
    for (Stmt unit : spec) {
      if (unit instanceof Stmt.OpDef) {
        Stmt.OpDef op = (Stmt.OpDef)unit;
        switch (op.name.lexeme) {
          case "Init"   : this.init = op;
          case "Next"   : this.next = op;
          case "Inv"    : this.invariantDef = op;
        }
      }
    }

    validate(this.init, "Init");
    validate(this.next, "Next");
    validate(this.invariantDef, "Inv");
  }

  private static void validate(Stmt.OpDef op, String name) {
    if (op == null) {
      throw new IllegalArgumentException(
          "Spec requires '" + name + "' operator.");
    }

    if (!op.params.isEmpty()) {
      throw new IllegalArgumentException(
          "Spec '" + name + "' operator cannot take parameters.");
    }
  }

  List<Map<String, Object>> checkSafety() {
    Deque<Map<String, Object>> pendingStates = new ArrayDeque<>();
    Map<Map<String, Object>, Map<String, Object>> predecessors = new HashMap<>();
    for (Map<String, Object> initialState : interpreter.getNextStates(init.name, init.body)) {
      pendingStates.add(initialState);
      predecessors.put(initialState, null);
    }

    TlaCallable invariant = (TlaCallable)interpreter.globals.get(invariantDef.name);
    while (!pendingStates.isEmpty()) {
      Map<String, Object> current = pendingStates.remove();
      interpreter.goToState(current);
      if (!(boolean)invariant.call(interpreter, new ArrayList<>())) {
        return reconstructStateTrace(predecessors, current);
      }

      for (Map<String, Object> next : interpreter.getNextStates(next.name, next.body)) {
        if (!predecessors.containsKey(next)) {
          predecessors.put(next, current);
          pendingStates.add(next);
        }
      }
    }

    return null;
  }

  List<Map<String, Object>> reconstructStateTrace(
      Map<Map<String, Object>, Map<String, Object>> predecessors,
      Map<String, Object> state
  ) {
    List<Map<String, Object>> trace = new ArrayList<>();
    Map<String, Object> predecessor = state;
    do {
      trace.add(predecessor);
      predecessor = predecessors.get(predecessor);
    } while (predecessor != null);
    return trace.reversed();
  }
}

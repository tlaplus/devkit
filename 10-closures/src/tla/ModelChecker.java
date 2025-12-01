package tla;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ModelChecker {

  record Step(String action, Map<String, Object> state) { }

  record StateTrace(String failingInvariant, List<Step> trace) { }

  private final Interpreter interpreter;
  private Stmt.OpDef init = null;
  private Stmt.OpDef next = null;
  private List<Stmt.OpDef> invariants = new ArrayList<>();

  ModelChecker(Interpreter interpreter, List<Stmt> spec) {
    this.interpreter = interpreter;
    for (Stmt unit : spec) {
      if (unit instanceof Stmt.OpDef op) {
        switch (op.name.lexeme) {
          case "Init"   -> init = op;
          case "Next"   -> next = op;
          case "Inv"    -> invariants.add(op);
          case "TypeOK" -> invariants.add(op);
          case "Safety" -> invariants.add(op);
        }
      }
    }

    validate(init, "Init");
    validate(next, "Next");
    for (Stmt.OpDef inv : invariants) validate(inv, inv.name.lexeme);
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

  StateTrace checkSafety() {
    Deque<Map<String, Object>> pendingStates = new ArrayDeque<>();
    Map<Map<String, Object>, Map<String, Object>> predecessors = new HashMap<>();
    for (Map<String, Object> initialState : interpreter.getNextStates(init.name, init.body)) {
      predecessors.put(initialState, null);
      pendingStates.add(initialState);
    }

    while (!pendingStates.isEmpty()) {
      Map<String, Object> current = pendingStates.remove();
      interpreter.goToState(current);
      for (Stmt.OpDef invariant : invariants) {
        if (!(boolean)invariant.body.accept(interpreter)) {
          return reconstructStateTrace(predecessors, current, invariant);
        }
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

  StateTrace reconstructStateTrace(
      Map<Map<String, Object>, Map<String, Object>> predecessors,
      Map<String, Object> state,
      Stmt.OpDef invariant
  ) {
    List<Map<String, Object>> trace = new ArrayList<>();
    Map<String, Object> predecessor = state;
    do {
      trace.add(predecessor);
      predecessor = predecessors.get(predecessor);
    } while (predecessor != null);
    trace = trace.reversed();

    List<Step> steps = new ArrayList<>();
    Stmt.OpDef action = init;
    for (Map<String, Object> nextState : trace) {
      steps.add(new Step(action.name.lexeme, nextState));
      action = next;
    }

    return new StateTrace(invariant.name.lexeme, steps);
  }
}

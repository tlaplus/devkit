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
    
    ModelChecker.validate(this.init, "Init");
    ModelChecker.validate(this.next, "Next");
    ModelChecker.validate(this.invariantDef, "Inv");
  }
  
  private final static void validate(Stmt.OpDef op, String name) {
    if (op == null) {
      throw new IllegalArgumentException("Spec requires '" + name + "' operator.");
    }
    
    if (!op.params.isEmpty()) {
      throw new IllegalArgumentException("Spec '" + name + "' operator cannot take parameters.");
    }
  }
  
  List<Map<String, Object>> checkSafety() {
    TlaCallable invariant = (TlaCallable)interpreter.globals.get(invariantDef.name);
    Deque<Map<String, Object>> stateQueue = new ArrayDeque<>();
    //Map<Integer, Integer> predecessors = new HashMap<>();
    Map<Map<String, Object>, Map<String, Object>> predecessors = new HashMap<>();
    try {
      interpreter.setCurrentState(null);
      stateQueue.addAll(interpreter.getNextStates(init.name, init.body));
      for (Map<String, Object> initialState : stateQueue) {
        predecessors.put(initialState, null);
      }
    } catch (RuntimeError e) {
      System.out.println("Error deriving initial states.");
      throw e;
    }
    
    while (!stateQueue.isEmpty()) {
      Map<String, Object> current = stateQueue.pop();
      interpreter.setCurrentState(current);
      if (!(boolean)invariant.call(interpreter, new ArrayList<>())) {
        return reconstructStateTrace(predecessors, current);
      }
      
      for (Map<String, Object> next : interpreter.getNextStates(next.name, next.body)) {
        if (!predecessors.containsKey(next)) {
          predecessors.put(next, current);
          stateQueue.add(next);
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
  
  List<Map<String, Object>> reconstructTrace(Map<Integer, Integer> predecessors, Map<String, Object> state) {
    List<Integer> hashTrace = reconstructHashTrace(predecessors, state);
    List<Map<String, Object>> trace = new ArrayList<>();
    interpreter.setCurrentState(null);
    List<Map<String, Object>> nextStates = interpreter.getNextStates(init.name, init.body);
    for (int stateHash : hashTrace) {
      for (Map<String, Object> nextState : nextStates) {
        if (nextState.hashCode() == stateHash) {
          trace.add(nextState);
          interpreter.setNextState(nextState);
          interpreter.step(next.name);
          break;
        }
      }
      
      nextStates = interpreter.getNextStates(next.name, next.body);
    }

    return trace;
  }
  
  List<Integer> reconstructHashTrace(Map<Integer, Integer> predecessors, Map<String, Object> state) {
    List<Integer> hashTrace = new ArrayList<>();
    Integer predecessorHash = state.hashCode();
    do {
      hashTrace.add(predecessorHash);
      predecessorHash = predecessors.get(predecessorHash);
    } while (predecessorHash != null);
    return hashTrace.reversed();
  }
}

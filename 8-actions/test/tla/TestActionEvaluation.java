package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestActionEvaluation {

  private static List<Stmt> parse(String input) {
    List<Stmt> statements = Utils.parse(input);
    for (Stmt statement : statements) {
      assertNotNull(statement, input);
    }

    return statements;
  }

  private static Stmt.Print parseAction(String input) {
    List<Stmt> statements = parse(input);
    assertEquals(1, statements.size());
    assertInstanceOf(Stmt.Print.class, statements.get(0));
    return (Stmt.Print)statements.get(0);
  }

  private static List<Map<String, Object>> getNextStates(String input, String actionInput) {
    try (IOCapture io = new IOCapture()) {
      Interpreter i = new Interpreter(true);
      i.interpret(parse(input));
      Stmt.Print action = parseAction(actionInput);
      return i.getNextStates(action.location, action.expression);
    }
  }

  @Test
  public void testVariableDeclarationErrors() {
    assertTrue(Utils.hasInterpreterError("VARIABLES x, y, x"), "Duplicate variable name");
    assertTrue(Utils.hasInterpreterError("x == 3 VARIABLE x"), "Variable name shadows operator name");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x == 3"), "Operator name shadows variable name");
    assertTrue(Utils.hasInterpreterError("VARIABLE z op(x, y, z) == x op(1, 2, 3)"), "Parameter name shadows variable name");
  }

  @Test
  public void testUninitializedVariableErrors() {
    // Unary ops
    assertTrue(Utils.hasInterpreterError("VARIABLE x -x"), "Uninitialized variable negative");
    assertTrue(Utils.hasInterpreterError("VARIABLE x ~x"), "Uninitialized variable ~");

    // Binary ops
    assertTrue(Utils.hasInterpreterError("VARIABLE x x + 1"), "Uninitialized variable +");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x - 1"), "Uninitialized variable minus");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x .. 2"), "Uninitialized variable ..");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x < 2"), "Uninitialized variable <");
    assertTrue(Utils.hasInterpreterError("VARIABLE x 2 \\in x"), "Uninitialized variable \\in");
    assertTrue(Utils.hasInterpreterError("VARIABLE x 2 = x"), "Uninitialized variable =");

    // Ternary ops
    assertTrue(Utils.hasInterpreterError("VARIABLE x IF x THEN 0 ELSE 1"), "Uninitialized variable ITE");

    // Variadic ops
    assertTrue(Utils.hasInterpreterError("VARIABLE x {x}"), "Uninitialized variable {}");
    assertTrue(Utils.hasInterpreterError("VARIABLE x {0, 1, x}"), "Uninitialized variable {}");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x /\\ TRUE"), "Uninitialized variable /\\");
    assertTrue(Utils.hasInterpreterError("VARIABLE x x \\/ TRUE"), "Uninitialized variable \\/");

    // Quantified functions
    assertTrue(Utils.hasInterpreterError("VARIABLE x [n \\in x |-> n]"), "Uninitialized variable |-> set");
    assertTrue(Utils.hasInterpreterError("VARIABLE x [n \\in {1} |-> x]"), "Uninitialized variable |-> body");
    assertTrue(Utils.hasInterpreterError("VARIABLE x \\A n \\in x : TRUE"), "Uninitialized variable \\A set");
    assertTrue(Utils.hasInterpreterError("VARIABLE x \\A n \\in {1} : x"), "Uninitialized variable \\A body");
    assertTrue(Utils.hasInterpreterError("VARIABLE x \\E n \\in x : TRUE"), "Uninitialized variable \\E set");
    assertTrue(Utils.hasInterpreterError("VARIABLE x \\E n \\in {1} : x"), "Uninitialized variable \\E body");

    // Function application
    assertTrue(Utils.hasInterpreterError("VARIABLE x x[0]"), "Uninitialized variable f[x] f");
    assertTrue(Utils.hasInterpreterError("VARIABLE x [v \\in {0} |-> v][x]"), "Uninitialized variable f[x] x");
  }

  @Test
  public void testInitialStateGeneration() {
    List<Map<String, Object>> expected = Arrays.asList(Map.of("x", 5));
    assertEquals(expected, getNextStates("VARIABLE x I == x = 5", "I"));
    expected = Arrays.asList(Map.of("x", true));
    assertEquals(expected, getNextStates("VARIABLE x I == x = TRUE", "I"));
    expected = Arrays.asList(Map.of("x", Map.of(0, 1, 1, 2, 2, 3)));
    assertEquals(expected, getNextStates("VARIABLE x I == x = [n \\in 0 .. 2 |-> n + 1]", "I"));
  }

  @Test
  public void testDisjunctionInitialStateGeneration() {
    List<Map<String, Object>> expected = Arrays.asList(Map.of("x", 0), Map.of("x", 1));
    assertEquals(expected, getNextStates("VARIABLE x I == x = 0 \\/ x = 1", "I"));
    expected = Arrays.asList(Map.of("x", true), Map.of("x", false));
    assertEquals(expected, getNextStates("VARIABLE x I == x = TRUE \\/ x = FALSE", "I"));
  }

  @Test
  public void testSetMembershipInitialStateGeneration() {
    List<Map<String, Object>> expected = Arrays.asList(Map.of("x", 0), Map.of("x", 1));
    assertEquals(expected, getNextStates("VARIABLE x I == x \\in {0, 1}", "I"));
    expected = Arrays.asList(Map.of("x", true), Map.of("x", false));
    assertEquals(expected, getNextStates("VARIABLE x I == x \\in {TRUE, FALSE}", "I"));
  }

  @Test
  public void testExistentialInitialStateGeneration() {
    List<Map<String, Object>> expected = Arrays.asList(Map.of("x", 0), Map.of("x", 1));
    assertEquals(expected, getNextStates("VARIABLE x I == \\E v \\in {0, 1} : x = v", "I"));
    expected = Arrays.asList(Map.of("x", true), Map.of("x", false));
    assertEquals(expected, getNextStates("VARIABLE x I == \\E v \\in {TRUE, FALSE} : x = v", "I"));
  }

  @Test
  public void testConjunctionInitialStateGeneration() {
    List<Map<String, Object>> expected = Arrays.asList(Map.of("x", 5, "y", true));
    assertEquals(expected, getNextStates("VARIABLES x, y I == x = 5 /\\ y = TRUE", "I"));
    expected = Arrays.asList(Map.of("x", true, "y", 1, "z", Map.of(0, 1, 1, 2, 2, 3)));
    assertEquals(expected, getNextStates("VARIABLES x, y, z I == x = TRUE /\\ y = 1 /\\ z = [n \\in 0 .. 2 |-> n + 1]", "I"));
    expected = Arrays.asList(Map.of("x", 0, "y", 1), Map.of("x", 1, "y", 0));
    String spec = """
        VARIABLES x, y
        Init ==
          \\/ /\\ x = 0
             /\\ y = 1
          \\/ /\\ x = 1
             /\\ y = 0
        """;
    assertEquals(expected, getNextStates(spec, "Init"));
    spec = """
        VARIABLES x, y
        Init ==
          /\\ x \\in {0, 1}
          /\\ y \\in {0, 1}
          /\\ ~(x = y)
        """;
    assertEquals(expected, getNextStates(spec, "Init"));
  }

  @Test
  public void testIncompleteInitialState() {
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y I == x = 5", "I"));
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y I == y = TRUE", "I"));
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y, z I == x = 5 /\\ z = TRUE", "I"));
  }

  @SafeVarargs
  private static void isTrace(String input, Map<String, Object>... states) {
    try (IOCapture io = new IOCapture()) {
      Interpreter i = new Interpreter(true);
      i.interpret(parse(input));

      boolean isInitialState = true;
      Stmt.Print init = parseAction("Init");
      Stmt.Print next = parseAction("Next");
      Stmt.Print inv = parseAction("Inv");
      for (Map<String, Object> state : states) {
        List<Map<String, Object>> nextStates =
            isInitialState
            ? i.getNextStates(init.location, init.expression)
            : i.getNextStates(next.location, next.expression);
        isInitialState = false;
        assertTrue(nextStates.contains(state), state.toString() + " not in " + nextStates.toString());
        i.step(state);
        assertTrue((boolean)i.executeBlock(inv.expression, i.globals));
      }
    }
  }

  @Test
  public void testSingleVarTraces() {
    String spec = """
        VARIABLE x
        Init == x = 0
        Next == x' = x + 1
        Inv == x \\in 0 .. 3
        """;
    isTrace(spec, Map.of("x", 0), Map.of("x", 1), Map.of("x", 2), Map.of("x", 3));

    spec = """
        VARIABLE t
        Init == t \\in {0, 1}
        Next == t' = 1 - t
        Inv == Init
        """;
    isTrace(spec, Map.of("t", 0), Map.of("t", 1), Map.of("t", 0), Map.of("t", 1));
    isTrace(spec, Map.of("t", 1), Map.of("t", 0), Map.of("t", 1), Map.of("t", 0));
  }

  @Test
  public void testMultiVarTraces() {
    String spec = """
        VARIABLES x, y
        Init ==
          /\\ x = 0
          /\\ y = TRUE
        Next ==
          /\\ x' \\in 0 .. 3
          /\\ y' = (~y)
        Inv ==
          /\\ x \\in 0 .. 3
          /\\ y \\in {TRUE, FALSE}
        """;
    isTrace(
        spec,
        Map.of("x", 0, "y", true),
        Map.of("x", 2, "y", false),
        Map.of("x", 2, "y", true),
        Map.of("x", 1, "y", false),
        Map.of("x", 3, "y", true),
        Map.of("x", 0, "y", false),
        Map.of("x", 2, "y", true)
    );
  }

  @Test
  public void testDisjunctionAndExistsTraces() {
    String spec = """
        VARIABLES x
        Init ==
          \\/ x = 0
          \\/ x = 5
        Next ==
          /\\ \\E n \\in 0 .. 3 : x' = x + n
        Inv == x \\in 0 .. 15
        """;
    isTrace(
        spec,
        Map.of("x", 5),
        Map.of("x", 5),
        Map.of("x", 6),
        Map.of("x", 9),
        Map.of("x", 12),
        Map.of("x", 12)
    );
    isTrace(
        spec,
        Map.of("x", 0),
        Map.of("x", 0),
        Map.of("x", 0),
        Map.of("x", 0),
        Map.of("x", 3),
        Map.of("x", 5),
        Map.of("x", 6)
    );
  }

  @Test
  public void testEnabled() {
    String spec = """
        VARIABLES x
        Init == x = 0
        Next == x' = x + 1
        Inv == ENABLED Next
        """;
    isTrace(
        spec,
        Map.of("x", 0),
        Map.of("x", 1),
        Map.of("x", 2)
    );
  }

  @Test
  public void testNotEnabled() {
    String spec = """
        VARIABLES x
        Init == x = 0
        Next ==
          /\\ x < 3
          /\\ x' = x + 1
        Inv ==
          \\/ x < 3 /\\ ENABLED Next
          \\/ x = 3 /\\ ~ENABLED Next
        """;
    isTrace(
        spec,
        Map.of("x", 0),
        Map.of("x", 1),
        Map.of("x", 2)
    );
  }
}

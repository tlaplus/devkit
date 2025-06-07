package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class TestActionEvaluation {

  private static List<Stmt> parse(String input) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), true);
    List<Stmt> statements = p.parse();
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
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Interpreter i = new Interpreter(new PrintStream(output), true);
    i.interpret(parse(input));

    Stmt.Print action = parseAction(actionInput);
    return i.getNextStates(action.location, action.expression);
  }

  private static String getRuntimeError(String input) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), true);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Interpreter i = new Interpreter(new PrintStream(output), true);
    PrintStream stderr = System.err;
    try {
      ByteArrayOutputStream errOutput = new ByteArrayOutputStream();
      System.setErr(new PrintStream(errOutput));
      i.interpret(p.parse());
      return errOutput.toString();
    } finally {
      System.setErr(stderr);
    }
  }

  @Test
  public void testVariableDeclarationErrors() {
    assertNotEquals("", getRuntimeError("VARIABLES x, y, x"), "Duplicate variable name");
    assertNotEquals("", getRuntimeError("x == 3 VARIABLE x"), "Variable name shadows operator name");
    assertNotEquals("", getRuntimeError("VARIABLE x x == 3"), "Operator name shadows variable name");
    assertNotEquals("", getRuntimeError("VARIABLE z op(x, y, z) == x op(1, 2, 3)"), "Parameter name shadows variable name");
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
  }

  @Test
  public void testIncompleteInitialState() {
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y I == x = 5", "I"));
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y I == y = TRUE", "I"));
    assertEquals(Arrays.asList(), getNextStates("VARIABLES x, y, z I == x = 5 /\\ z = TRUE", "I"));
  }
}

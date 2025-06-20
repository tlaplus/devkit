package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestStatementEvaluation {

  @Test
  public void testLiterals() {
    assertEquals("5", Utils.interpret("x == 2 y == 3 x + y"));
    assertEquals("1", Utils.interpret("x == TRUE IF x THEN 1 ELSE 2"));
  }

  @Test
  public void testCannotCallVariable() {
    assertTrue(Utils.hasInterpreterError("VARIABLE x x(1)"));
    assertFalse(Utils.hasInterpreterError("VARIABLE x x = 1"));
    assertFalse(Utils.hasInterpreterError("VARIABLE x x = 1 x"));
    assertTrue(Utils.hasInterpreterError("VARIABLE x x = 1 x(1)"));
  }
}

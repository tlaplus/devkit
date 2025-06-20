package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestActionParsing {

  private static void checkEqual(String input, String expected) {
    String actual = Utils.parseToSExpr(input);
    assertEquals(expected, actual);
  }

  @Test
  public void testVariableDeclaration() {
    checkEqual("VARIABLE x", "(VARIABLE x)");
    checkEqual("VARIABLES x", "(VARIABLE x)");
    checkEqual("VARIABLES x, y, z", "(VARIABLE x y z)");
  }
}
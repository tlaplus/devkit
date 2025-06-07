package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestActionParsing {

  private static void checkEqual(boolean replMode, String input, String expected) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), replMode);
    String actual = new AstPrinter().print(p.parse());
    assertEquals(expected, actual);
  }

  @Test
  public void testVariableDeclaration() {
    checkEqual(false, "VARIABLE x", "(VARIABLE x)");
    checkEqual(false, "VARIABLES x", "(VARIABLE x)");
    checkEqual(false, "VARIABLES x, y, z", "(VARIABLE x y z)");
  }
}
package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestStatementParsing {

  private static void checkEqual(boolean replMode, String input, String expected) {
    try (IOCapture io = new IOCapture()) {
      Scanner s = new Scanner(input);
      Parser p = new Parser(s.scanTokens(), replMode);
      String actual = new AstPrinter().print(p.parse());
      assertEquals(expected, actual);
    }
  }

  @Test
  public void testOperatorDefinition() {
    checkEqual(false, "op == 123", "(op 123)");
    checkEqual(false, "op == 123 op2 == TRUE", "(op 123) (op2 true)");
    checkEqual(true, "op == 123 3 + 5", "(op 123) (print (+ 3 5))");
  }
}

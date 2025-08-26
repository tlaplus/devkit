package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestStatementParsing {

  private static void checkEqual(String input, String expected) {
    assertEquals(expected, Utils.parseToSExpr(input));
  }

  @Test
  public void testOperatorDefinition() {
    checkEqual("op == 123", "(op 123)");
    checkEqual("op == 123 op2 == TRUE", "(op 123) (op2 true)");
    checkEqual("op == 123 3 + 5", "(op 123) (print (+ 3 5))");
  }
}

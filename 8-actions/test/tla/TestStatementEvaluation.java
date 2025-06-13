package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestStatementEvaluation {
  private static String interpret(String input) {
    try (IOCapture io = new IOCapture()) {
      Scanner s = new Scanner(input);
      Parser p = new Parser(s.scanTokens(), true);
      Interpreter i = new Interpreter(System.out, true);
      i.interpret(p.parse());
      return io.getOut().strip();
    }
  }

  @Test
  public void testLiterals() {
    assertEquals("5", interpret("x == 2 y == 3 x + y"));
    assertEquals("1", interpret("x == TRUE IF x THEN 1 ELSE 2"));
  }
}
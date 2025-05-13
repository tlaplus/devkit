package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

public class TestStatementEvaluation {
  private static String interpret(String input) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), true);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Interpreter i = new Interpreter(new PrintStream(output), true);
    i.interpret(p.parse());
    return output.toString().strip();
  }

  @Test
  public void testLiterals() {
    assertEquals("5", interpret("x == 2 y == 3 x + y"));
    assertEquals("1", interpret("x == TRUE IF x THEN 1 ELSE 2"));
  }
}
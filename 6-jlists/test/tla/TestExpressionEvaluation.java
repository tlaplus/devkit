package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestExpressionEvaluation {
  private static String interpret(String input) {
    try (IOCapture io = new IOCapture()) {
      Scanner s = new Scanner(input);
      Parser p = new Parser(s.scanTokens(), true);
      Interpreter i = new Interpreter(true);
      i.interpret(p.parse());
      return io.getOut().strip();
    }
  }

  @Test
  public void testLiterals() {
    assertEquals("true", interpret("TRUE"));
    assertEquals("false", interpret("FALSE"));
    assertEquals("123", interpret("123"));
  }

  @Test
  public void testUnary() {
    assertEquals("false", interpret("~TRUE"));
    assertEquals("true", interpret("\\lnot FALSE"));
    assertEquals("false", interpret("\\neg TRUE"));
    assertEquals("false", interpret("ENABLED TRUE"));
    assertEquals("false", interpret("ENABLED FALSE"));
    assertEquals("-4", interpret("-4"));
    assertEquals("0", interpret("-0"));
    assertEquals("10", interpret("--10"));
    assertEquals("true", interpret("TRUE'"));
    assertEquals("false", interpret("FALSE'"));
    assertEquals("1", interpret("1'"));
  }

  @Test
  public void testBinary() {
    assertEquals("3", interpret("1 + 2"));
    assertEquals("-1", interpret("1 - 2"));
    assertEquals("false", interpret("TRUE /\\ FALSE"));
    assertEquals("true", interpret("TRUE /\\ TRUE"));
    assertEquals("true", interpret("TRUE \\/ FALSE"));
    assertEquals("false", interpret("FALSE \\/ FALSE"));
    assertEquals("true", interpret("1 \\in {1,2,3}"));
    assertEquals("[1, 2, 3]", interpret("1 .. 3"));
    assertEquals("[]", interpret("3 .. 1"));
    assertEquals("[3]", interpret("3 .. 3"));
    assertEquals("true", interpret("1 < 2"));
    assertEquals("false", interpret("2 < 1"));
    assertEquals("true", interpret("1 = 1"));
    assertEquals("false", interpret("1 = 2"));
    assertEquals("true", interpret("FALSE = FALSE"));
    assertEquals("false", interpret("FALSE = TRUE"));
    assertEquals("true", interpret("(1 .. 3) = {1, 2, 3}"));
    assertEquals("false", interpret("(1 .. 3) = {1, 2}"));
  }

  @Test
  public void testShortCircuitEvaluation() {
    assertEquals("false", interpret("FALSE /\\ 123"));
  }

  @Test
  public void testGrouping() {
    assertEquals("-4", interpret("1 - 2 - 3"));
    assertEquals("2", interpret("1 - (2 - 3)"));
  }

  @Test
  public void testTernary() {
    assertEquals("1", interpret("IF TRUE THEN 1 ELSE 2"));
    assertEquals("2", interpret("IF FALSE THEN 1 ELSE 2"));
  }

  @Test
  public void testVariadic() {
    assertEquals("[]", interpret("{}"));
    assertEquals("[1, 2, 3]", interpret("{1, 2, 3}"));
    assertEquals("[1, 2, 3]", interpret("{1, 2, 3, 1, 2, 3}"));
    assertEquals("[1, 2, 3, false, true]", interpret("{1, 2, 3, 1, 2, 3, TRUE, TRUE, FALSE, FALSE}"));
    assertEquals("[[]]", interpret("{{},{},{}}"));
    assertEquals("[[], [[]]]", interpret("{{},{{}},{}}"));
  }

  @Test
  public void testSetOperations() {
    assertEquals("true", interpret("{} \\in {{}}"));
    assertEquals("false", interpret("{} \\in {{{}}}"));
    assertEquals("true", interpret("{} = {}"));
    assertEquals("true", interpret("{{}, 0..2} = {{0,1,2}, {}}"));
    assertEquals("true", interpret("{TRUE, 0..2, {{}}} = {{0,1,2}, ~FALSE, {{}}}"));
    assertEquals("true", interpret("{TRUE, 0..2, {{}}} \\in {{{0,1,2}, ~FALSE, {{}}}}"));
  }

  @Test
  public void testEquality() {
    assertEquals("false", interpret("1 = TRUE"));
    assertEquals("false", interpret("FALSE = 2"));
    assertEquals("false", interpret("1 = {}"));
    assertEquals("false", interpret("TRUE = {}"));
  }
}

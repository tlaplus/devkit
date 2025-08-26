package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestExpressionEvaluation {

  @Test
  public void testLiterals() {
    assertEquals("true", Utils.interpret("TRUE"));
    assertEquals("false", Utils.interpret("FALSE"));
    assertEquals("123", Utils.interpret("123"));
  }

  @Test
  public void testUnary() {
    assertEquals("false", Utils.interpret("~TRUE"));
    assertEquals("true", Utils.interpret("\\lnot FALSE"));
    assertEquals("false", Utils.interpret("\\neg TRUE"));
    assertEquals("false", Utils.interpret("ENABLED TRUE"));
    assertEquals("false", Utils.interpret("ENABLED FALSE"));
    assertEquals("-4", Utils.interpret("-4"));
    assertEquals("0", Utils.interpret("-0"));
    assertEquals("10", Utils.interpret("--10"));
  }

  @Test
  public void testBinary() {
    assertEquals("3", Utils.interpret("1 + 2"));
    assertEquals("-1", Utils.interpret("1 - 2"));
    assertEquals("false", Utils.interpret("TRUE /\\ FALSE"));
    assertEquals("true", Utils.interpret("TRUE /\\ TRUE"));
    assertEquals("true", Utils.interpret("TRUE \\/ FALSE"));
    assertEquals("false", Utils.interpret("FALSE \\/ FALSE"));
    assertEquals("true", Utils.interpret("1 \\in {1,2,3}"));
    assertEquals("[1, 2, 3]", Utils.interpret("1 .. 3"));
    assertEquals("[]", Utils.interpret("3 .. 1"));
    assertEquals("[3]", Utils.interpret("3 .. 3"));
    assertEquals("true", Utils.interpret("1 < 2"));
    assertEquals("false", Utils.interpret("2 < 1"));
    assertEquals("true", Utils.interpret("1 = 1"));
    assertEquals("false", Utils.interpret("1 = 2"));
    assertEquals("true", Utils.interpret("FALSE = FALSE"));
    assertEquals("false", Utils.interpret("FALSE = TRUE"));
    assertEquals("true", Utils.interpret("(1 .. 3) = {1, 2, 3}"));
    assertEquals("false", Utils.interpret("(1 .. 3) = {1, 2}"));
  }

  @Test
  public void testShortCircuitEvaluation() {
    assertEquals("false", Utils.interpret("FALSE /\\ 123"));
  }

  @Test
  public void testGrouping() {
    assertEquals("-4", Utils.interpret("1 - 2 - 3"));
    assertEquals("2", Utils.interpret("1 - (2 - 3)"));
  }

  @Test
  public void testTernary() {
    assertEquals("1", Utils.interpret("IF TRUE THEN 1 ELSE 2"));
    assertEquals("2", Utils.interpret("IF FALSE THEN 1 ELSE 2"));
  }

  @Test
  public void testVariadic() {
    assertEquals("[]", Utils.interpret("{}"));
    assertEquals("[1, 2, 3]", Utils.interpret("{1, 2, 3}"));
    assertEquals("[1, 2, 3]", Utils.interpret("{1, 2, 3, 1, 2, 3}"));
    assertEquals("[1, 2, 3, false, true]", Utils.interpret("{1, 2, 3, 1, 2, 3, TRUE, TRUE, FALSE, FALSE}"));
    assertEquals("[[]]", Utils.interpret("{{},{},{}}"));
    assertEquals("[[], [[]]]", Utils.interpret("{{},{{}},{}}"));
  }

  @Test
  public void testSetOperations() {
    assertEquals("true", Utils.interpret("{} \\in {{}}"));
    assertEquals("false", Utils.interpret("{} \\in {{{}}}"));
    assertEquals("true", Utils.interpret("{} = {}"));
    assertEquals("true", Utils.interpret("{{}, 0..2} = {{0,1,2}, {}}"));
    assertEquals("true", Utils.interpret("{TRUE, 0..2, {{}}} = {{0,1,2}, ~FALSE, {{}}}"));
    assertEquals("true", Utils.interpret("{TRUE, 0..2, {{}}} \\in {{{0,1,2}, ~FALSE, {{}}}}"));
  }

  @Test
  public void testEquality() {
    assertEquals("false", Utils.interpret("1 = TRUE"));
    assertEquals("false", Utils.interpret("FALSE = 2"));
    assertEquals("false", Utils.interpret("1 = {}"));
    assertEquals("false", Utils.interpret("TRUE = {}"));
  }
}

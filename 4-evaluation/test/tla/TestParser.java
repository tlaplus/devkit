package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

public class TestParser {

  private static void checkEqual(String input, String expected) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens());
    String actual = new AstPrinter().print(p.parse());
    assertEquals(expected, actual);
  }

  private static void checkError(String input) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens());
    assertNull(p.parse());
  }

  @Test
  public void testLiteralExpressions() {
    checkEqual("TRUE", "true");
    checkEqual("FALSE", "false");
    checkEqual("123", "123");
  }

  @Test
  public void testUnaryExpressions() {
    checkEqual("~1", "(~ 1)");
    checkEqual("\\lnot 1", "(\\lnot 1)");
    checkEqual("\\neg 1", "(\\neg 1)");
    checkEqual("-1", "(- 1)");
    checkEqual("1'", "(' 1)");
    checkEqual("ENABLED 1", "(ENABLED 1)");
  }

  @Test
  public void testUnaryAssociativity() {
    checkEqual("~~1", "(~ (~ 1))");
    checkEqual("--1", "(- (- 1))");
  }

  @Test
  public void testBinaryExpressions() {
    checkEqual("1 \\in 1", "(\\in 1 1)");
    checkEqual("1 = 1", "(= 1 1)");
    checkEqual("1 < 1", "(< 1 1)");
    checkEqual("1 .. 1", "(.. 1 1)");
    checkEqual("1 + 1", "(+ 1 1)");
    checkEqual("1 - 1", "(- 1 1)");
  }

  @Test
  public void testBinaryAssociativity() {
    checkEqual("1 + 2 + 3", "(+ (+ 1 2) 3)");
    checkEqual("1 - 2 - 3", "(- (- 1 2) 3)");
  }

  @Test
  public void testPrecedence() {
    checkEqual("1 < 2 .. 3", "(< 1 (.. 2 3))");
    checkEqual("1 < 2 + 3", "(< 1 (+ 2 3))");
    checkEqual("1 + 2 - 3", "(+ 1 (- 2 3))");
    checkEqual("1 + 2'", "(+ 1 (' 2))");
    checkEqual("~1'", "(~ (' 1))");
    checkEqual("-1'", "(- (' 1))");
    checkEqual("-1 + 2'", "(+ (- 1) (' 2))");
  }

  @Test
  public void testAssociativityErrors() {
    checkError("ENABLED ENABLED TRUE");
  }

  @Test
  public void testGroupingExpressions() {
    checkEqual("(1)", "(group 1)");
    checkEqual("(1 + (2 + 3))", "(group (+ 1 (group (+ 2 3))))");
  }

  @Test
  public void testIfThenElse() {
    checkEqual("IF TRUE THEN 1 ELSE 2", "(IF true 1 2)");
    checkEqual("IF IF TRUE THEN 1 ELSE 2 THEN 1 ELSE 2", "(IF (IF true 1 2) 1 2)");
    checkEqual("IF TRUE THEN IF TRUE THEN 1 ELSE 2 ELSE 2", "(IF true (IF true 1 2) 2)");
    checkEqual("IF TRUE THEN 1 ELSE IF TRUE THEN 1 ELSE 2", "(IF true 1 (IF true 1 2))");
  }

  @Test
  public void testSetConstructor() {
    checkEqual("{}", "({)");
    checkEqual("{1,2,3}", "({ 1 2 3)");
    checkEqual("{{},{},{}}", "({ ({) ({) ({))");
    checkEqual("{{1,2,3},{1,2},{1},{}}", "({ ({ 1 2 3) ({ 1 2) ({ 1) ({))");
    checkEqual("{{{{}}}}", "({ ({ ({ ({))))");
  }
}

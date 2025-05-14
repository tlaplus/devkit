package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestOperatorParsing {

  private static void checkEqual(boolean replMode, String input, String expected) {
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), replMode);
    String actual = new AstPrinter().print(p.parse());
    assertEquals(expected, actual);
  }

  @Test
  public void testOperatorDefinition() {
    checkEqual(false, "op(x) == x", "(op x (x))");
    checkEqual(false, "op(x, y, z) == x + y + z", "(op x y z (+ (+ (x) (y)) (z)))");
  }

  @Test
  public void testQuantification() {
    checkEqual(true, "\\A x \\in S : f(x)", "(print (\\A x (S) (f (x))))");
    checkEqual(true, "\\A x, y, z \\in S : f(x)", "(print (\\A x y z (S) (f (x))))");
    checkEqual(true, "\\E x \\in S : f(x)", "(print (\\E x (S) (f (x))))");
    checkEqual(true, "\\E x, y, z \\in S : f(x)", "(print (\\E x y z (S) (f (x))))");
    checkEqual(true, "\\E x \\in S : \\A y \\in S : f(x, y)", "(print (\\E x (S) (\\A y (S) (f (x) (y)))))");
  }

  @Test
  public void testFunctions() {
    checkEqual(true, "[x \\in S |-> f(x)]", "(print (|-> x (S) (f (x))))");
    checkEqual(true, "[x \\in S |-> [y \\in S |-> f(x, y)]]", "(print (|-> x (S) (|-> y (S) (f (x) (y)))))");
    checkEqual(true, "[x \\in S |-> f(x)][1]", "(print ((|-> x (S) (f (x))) 1))");
    checkEqual(true, "[x \\in S |-> [y \\in S |-> f(x, y)]][1][2]", "(print (((|-> x (S) (|-> y (S) (f (x) (y)))) 1) 2))");
  }
}

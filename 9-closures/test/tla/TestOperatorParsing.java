package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestOperatorParsing {

  private static void checkEqual(String input, String expected) {
    assertEquals(expected, Utils.parseToSExpr(input));
  }

  @Test
  public void testOperatorDefinition() {
    checkEqual("op(x) == x", "(op x (x))");
    checkEqual("op(x, y, z) == x + y + z", "(op x y z (+ (+ (x) (y)) (z)))");
  }

  @Test
  public void testQuantification() {
    checkEqual("\\A x \\in S : f(x)", "(print (\\A x (S) (f (x))))");
    checkEqual("\\A x, y, z \\in S : f(x)", "(print (\\A x y z (S) (f (x))))");
    checkEqual("\\E x \\in S : f(x)", "(print (\\E x (S) (f (x))))");
    checkEqual("\\E x, y, z \\in S : f(x)", "(print (\\E x y z (S) (f (x))))");
    checkEqual("\\E x \\in S : \\A y \\in S : f(x, y)", "(print (\\E x (S) (\\A y (S) (f (x) (y)))))");
  }

  @Test
  public void testFunctions() {
    checkEqual("[x \\in S |-> f(x)]", "(print (|-> x (S) (f (x))))");
    checkEqual("[x \\in S |-> [y \\in S |-> f(x, y)]]", "(print (|-> x (S) (|-> y (S) (f (x) (y)))))");
    checkEqual("[x \\in S |-> f(x)][1]", "(print ((|-> x (S) (f (x))) 1))");
    checkEqual("[x \\in S |-> [y \\in S |-> f(x, y)]][1][2]", "(print (((|-> x (S) (|-> y (S) (f (x) (y)))) 1) 2))");
  }
}

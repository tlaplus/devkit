package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestOperatorEvaluation {

  @Test
  public void testOperatorDefinitions() {
    assertEquals("3", Utils.interpret("f(x) == x + 1 f(2)"));
    assertEquals("5", Utils.interpret("f(x, y) == x + y f(2, 3)"));
    assertEquals("true", Utils.interpret("f(x, y) == x < y f(2, 3)"));
  }

  @Test
  public void testOperatorErrors() {
    assertTrue(Utils.hasInterpreterError("op(op) == 3"), "Operator name as own parameter");
    assertTrue(Utils.hasInterpreterError("op(x, y, op) == 3"), "Operator name as own parameter");
    assertTrue(Utils.hasInterpreterError("op(x, y, x) == 3"), "Duplicated parameter name");
    assertTrue(Utils.hasInterpreterError("op == 3 op2(op) == 3"), "Operator name as op parameter");
  }

  @Test
  public void testQuantification() {
    assertEquals("true", Utils.interpret("\\A x \\in 0 .. 2 : x < 3"));
    assertEquals("false", Utils.interpret("\\A x, y \\in 0 .. 2 : x = y"));
    assertEquals("false", Utils.interpret("\\A x, y, z \\in 0 .. 2 : (x + y + z) < 6"));
    assertEquals("true", Utils.interpret("\\A x, y, z \\in 0 .. 2 : (x + y + z) < 7"));
    assertEquals("true", Utils.interpret("\\E x \\in 0 .. 2 : x = 1"));
    assertEquals("true", Utils.interpret("\\E x, y \\in 0 .. 2 : x = y"));
    assertEquals("true", Utils.interpret("\\E x, y, z \\in 0 .. 2 : (x + y + z) = 5"));
    assertEquals("false", Utils.interpret("\\E x, y, z \\in 0 .. 2 : (x + y + z) = 7"));
    assertEquals("true", Utils.interpret("S == 0 .. 2 \\E x \\in S : \\A y \\in S : x = y \\/ x < y"));
  }

  @Test
  public void testFunctions() {
    assertEquals("{0=1, 1=2, 2=3}", Utils.interpret("[x \\in 0 .. 2 |-> x + 1]"));
    assertEquals("{0={false=true, true=false}, 1={false=true, true=false}, 2={false=true, true=false}}", Utils.interpret("[x \\in 0 .. 2 |-> [y \\in {TRUE, FALSE} |-> ~y]]"));
    assertEquals("2", Utils.interpret("[x \\in 0 .. 2 |-> x + 1][1]"));
    assertEquals("2", Utils.interpret("f == [x \\in 0 .. 2 |-> x + 1] f[1]"));
    assertEquals("false", Utils.interpret("f == [x \\in 0 .. 2 |-> [y \\in {TRUE, FALSE} |-> ~y]] f[2][TRUE]"));
  }

  @Test
  public void testFunctionErrors() {
    assertTrue(Utils.hasInterpreterError("op == 3 \\A op \\in {} : TRUE"), "Operator name as universally-quantified variable");
    assertTrue(Utils.hasInterpreterError("op(x) == \\A x \\in {} : TRUE op(1)"), "Parameter name as universally-quantified variable");
    assertTrue(Utils.hasInterpreterError("op(y) == \\A x, y \\in {} : TRUE op(1)"), "Parameter name as universally-quantified variable");
    assertTrue(Utils.hasInterpreterError("op == 3 \\E x, op \\in {} : TRUE"), "Operator name as existentially-quantified variable");
    assertTrue(Utils.hasInterpreterError("op(x) == \\E x \\in {} : TRUE op(1)"), "Parameter name as existentially-quantified variable");
    assertTrue(Utils.hasInterpreterError("op(y) == \\E x, y \\in {} : TRUE op(1)"), "Parameter name as existentially-quantified variable");
    assertTrue(Utils.hasInterpreterError("op == 3 op2 == [op \\in {} |-> 1] op2"), "Operator name as function parameter");
    assertTrue(Utils.hasInterpreterError("op(x) == [x \\in {} |-> 1] op(1)"), "Parameter name as function parameter");
  }

  @Test
  public void testParameterizedQuantification() {
    assertEquals("true", Utils.interpret("op(n, m) == \\A x \\in 0 .. n : x < m op(2, 3)"));
    assertEquals("false", Utils.interpret("op(n, m) == \\A x \\in 0 .. n : x < m op(3, 3)"));
    assertEquals("true", Utils.interpret("op(n, m) == \\E x \\in 0 .. n : x < m op(3, 1)"));
    assertEquals("false", Utils.interpret("op(n, m) == \\E x \\in 0 .. n : x < m op(3, 0)"));
    assertEquals("true", Utils.interpret("op(n, m) == \\A x \\in 0 .. n : \\E y \\in 0 .. m : x = y op(2, 3)"));
    assertEquals("true", Utils.interpret("op(n, m) == \\A x \\in 0 .. n : \\E y \\in 0 .. m : x = y op(4, 4)"));
    assertEquals("false", Utils.interpret("op(n, m) == \\A x \\in 0 .. n : \\E y \\in 0 .. m : x = y op(4, 3)"));
    assertEquals("true", Utils.interpret("op(n, m) == \\A x, y \\in 0 .. n : \\E z \\in 0 .. m : (x + y) = z op(2, 4)"));
    assertEquals("false", Utils.interpret("op(n, m) == \\A x, y \\in 0 .. n : \\E z \\in 0 .. m : (x + y) = z op(2, 3)"));
  }

  @Test
  public void testQuantifiedOperator() {
    assertEquals("false", Utils.interpret("op(n, m) == n < m q(n) == \\A x \\in 0 .. n : op(x, n) q(3)"));
    assertEquals("true", Utils.interpret("op(n, m) == n < m q(n) == \\A x \\in 0 .. n : op(x, n + 1) q(3)"));
  }

  @Test
  public void testParameterizedFunctions() {
    assertEquals("5", Utils.interpret("op(n, m) == [x \\in 0 .. n |-> x + m] op(2, 3)[2]"));
    assertEquals("5", Utils.interpret("op(n, m) == [x \\in 0 .. n |-> x + m] f(n, m, x) == op(n, m)[x] f(2, 3, 2)"));
    assertEquals("false", Utils.interpret("f(n, e) == [x \\in 0 .. n |-> [y \\in {TRUE, FALSE} |-> ~y]][e] f(2, 1)[TRUE]"));
  }

  @Test
  public void testFunctionOperator() {
    assertEquals("4", Utils.interpret("f(n) == n + 1 op(n) == [x \\in 0 .. n |-> f(x)] op(3)[3]"));
  }
}

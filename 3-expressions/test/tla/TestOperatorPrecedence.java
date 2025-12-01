package tla;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestOperatorPrecedence {

  @DisplayName ("Test with @MethodSource")
  @ParameterizedTest(name = "{index}: {0}, {1}" )
  @MethodSource("generateOperatorCombinations")
  public void test(Parser.Operator left, Parser.Operator right) {
    assumeFalse(left.fix() == Parser.Fix.POSTFIX && right.fix() == Parser.Fix.PREFIX);
    final boolean conflicts = conflictsWith(left, right);
    final String input = deriveExpression(left, right);
    final String msg = String.format("%s|%s|%s|%b\n", left.token(), right.token(), input, conflicts);
    System.out.println(msg);
    try (IOCapture io = new IOCapture()) {
      Scanner s = new Scanner(input);
      Parser p = new Parser(s.scanTokens());
      Expr actual = p.parse();
      if (conflicts) {
        if (actual != null) {
          assertFalse(isCompleteParse(actual), msg);
        }
      } else {
        assertNotNull(actual, msg + io.getErr());
        assertTrue(isCompleteParse(actual), msg);
        final TokenType lowerPrecOpSymbol = getOpToken(actual);
        final TokenType higherPrecOpSymbol = getHigherPrecOp(actual);
        if (left == right) {
          if (left.assoc()) {
            assertEquals(lowerPrecOpSymbol, right.token(), msg);
            assertEquals(higherPrecOpSymbol, left.token(), msg);
          } else {
            assertEquals(lowerPrecOpSymbol, left.token(), msg);
            assertEquals(higherPrecOpSymbol, right.token(), msg);
          }
        } else if (isLowerPrecThan(left, right) || Parser.Fix.PREFIX == right.fix()) {
          assertEquals(lowerPrecOpSymbol, left.token(), msg);
          assertEquals(higherPrecOpSymbol, right.token(), msg);
        } else {
          assertEquals(lowerPrecOpSymbol, right.token(), msg);
          assertEquals(higherPrecOpSymbol, left.token(), msg);
        }
      }
    }
  }

  static Stream<Arguments> generateOperatorCombinations() {
    return
      Stream.of(Parser.operators)
      .flatMap(left ->
        Stream.of(Parser.operators)
        .map(right -> Arguments.of(left, right)));
  }

  private static String deriveExpression(Parser.Operator left, Parser.Operator right) {
    final String leftSymbol = getOpSymbol(left);
    final String rightSymbol = getOpSymbol(right);
    return switch (left.fix()) {
      case PREFIX -> switch (right.fix()) {
        case PREFIX -> String.format("%s %s 1", leftSymbol, rightSymbol);
        case INFIX -> String.format("%s 1 %s 2", leftSymbol, rightSymbol);
        case POSTFIX -> String.format("%s 1 %s", leftSymbol, rightSymbol);
      };
      case INFIX -> switch (right.fix()) {
        case PREFIX -> String.format("1 %s %s 2", leftSymbol, rightSymbol);
        case INFIX -> String.format("1 %s 2 %s 3", leftSymbol, rightSymbol);
        case POSTFIX -> String.format("1 %s 2 %s", leftSymbol, rightSymbol);
      };
      case POSTFIX -> switch (right.fix()) {
        case PREFIX -> throw new RuntimeException("Cannot derive expression between postfix and prefix operators");
        case INFIX -> String.format("1 %s %s 2", leftSymbol, rightSymbol);
        case POSTFIX -> String.format("1 %s %s", leftSymbol, rightSymbol);
      };
    };
  }

  private static String getOpSymbol(Parser.Operator op) {
    return switch (op.token()) {
      case NOT -> "~";
      case ENABLED -> "ENABLED";
      case MINUS -> "-";
      case IN -> "\\in";
      case EQUAL -> "=";
      case LESS_THAN -> "<";
      case DOT_DOT -> "..";
      case PLUS -> "+";
      case PRIME -> "'";
      default -> throw new RuntimeException("Unknown operator symbol " + op.token().toString());
    };
  }

  private static boolean conflictsWith(Parser.Operator left, Parser.Operator right) {
    if (left.fix() == right.fix() && (Parser.Fix.PREFIX == left.fix() || Parser.Fix.POSTFIX == right.fix())) {
      // Prefix & postfix ops can't really conflict with others in their class.
      return false;
    } else if (Parser.Fix.INFIX == left.fix() && Parser.Fix.PREFIX == right.fix()) {
      // Expressions such as A + -B are always unambiguous.
      return false;
    } else if (Parser.Fix.POSTFIX == left.fix() && Parser.Fix.INFIX == right.fix()) {
      // Expressions such as A' + B are always unambiguous.
      return false;
    } else if (left == right) {
      // Identical infix ops will conflict unless they are associative.
      return !left.assoc();
    } else {
      // Conflicts if precedence range overlaps.
      return (left.lowPrec() <= right.lowPrec() && right.lowPrec() <= left.highPrec()) // overlap low
          || (left.lowPrec() <= right.lowPrec() && right.highPrec() <= left.highPrec()) // enclose
          || (right.lowPrec() <= left.lowPrec() && left.highPrec() <= right.highPrec()) // enclosed by
          || (left.lowPrec() <= right.highPrec() && right.highPrec() <= left.highPrec()); // overlap high
    }
  }
  
  private static boolean isLowerPrecThan(Parser.Operator left, Parser.Operator right) {
    return left.lowPrec() < right.lowPrec()
        && left.highPrec() < right.highPrec();
  }
  
  private static TokenType getHigherPrecOp(Expr expr) {
    if (expr instanceof Expr.Binary bExpr) {
      return bExpr.left instanceof Expr.Literal ? getOpToken(bExpr.right) : getOpToken(bExpr.left);
    } else if (expr instanceof Expr.Unary uExpr) {
      return getOpToken(uExpr.expr);
    } else {
      throw new RuntimeException("Invalid expression type: " + expr.getClass().descriptorString());
    }
  }
  
  private boolean isCompleteParse(Expr expr) {
    if (expr instanceof Expr.Binary bExpr) {
      return !(bExpr.left instanceof Expr.Literal && bExpr.right instanceof Expr.Literal);
    } else if (expr instanceof Expr.Unary uExpr) {
      return !(uExpr.expr instanceof Expr.Literal);
    } else {
      throw new RuntimeException("Invalid expression type: " + expr.getClass().descriptorString());
    }
  }

  private static TokenType getOpToken(Expr expr) {
    if (expr instanceof Expr.Binary bExpr) {
      return bExpr.operator.type;
    } else if (expr instanceof Expr.Unary uExpr) {
      return uExpr.operator.type;
    } else {
      throw new RuntimeException("Invalid expression type: " + expr.getClass().descriptorString());
    }
  }
}
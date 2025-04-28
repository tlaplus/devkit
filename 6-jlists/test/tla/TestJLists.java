package tla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TestJLists {

  private static void checkEqual(String input, String expected) {
    input = "op ==\n" + input;
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), false);
    List<Stmt> statements = p.parse();
    for (Stmt statement : statements) {
      assertNotNull(statement, input);
    }
    String actual = new AstPrinter().print(statements);
    assertEquals("(op " + expected + ")", actual, input);
  }

  @Test
  public void testBasicJList() {
    checkEqual(
        """
          /\\ 1
          /\\ 2
          /\\ 3
        """,
        "(/\\ 1 2 3)"
    );
    checkEqual(
        """
          \\/ 1
          \\/ 2
          \\/ 3
        """,
        "(\\/ 1 2 3)"
    );
  }

  @Test
  public void testStartOfLineJList() {
    checkEqual(
        """
        /\\ 1
        /\\ 2
        /\\ 3
        """,
        "(/\\ 1 2 3)"
    );
    checkEqual(
        """
        \\/ 1
        \\/ 2
        \\/ 3
        """,
        "(\\/ 1 2 3)"
    );
  }

  @Test
  public void testLeftShiftedJList() {
    checkEqual(
        """
          /\\ 1
        /\\ 2
        """,
        "(/\\ (/\\ 1) 2)"
    );
    checkEqual(
        """
          \\/ 1
        \\/ 2
        """,
        "(\\/ (\\/ 1) 2)"
    );
  }

  @Test
  public void testRightShiftedJList() {
    checkEqual(
        """
        /\\ 1
          /\\ 2
        """,
        "(/\\ (/\\ 1 2))"
    );
    checkEqual(
        """
        \\/ 1
          \\/ 2
        """,
        "(\\/ (\\/ 1 2))"
    );
  }

  @Test
  public void testSeparatedJList() {
    checkEqual(
        """
          /\\ 1

          /\\ 2
        """,
        "(/\\ 1 2)"
    );
    checkEqual(
        """
          \\/ 1

          \\/ 2
        """,
        "(\\/ 1 2)"
    );
  }

  @Test
  public void testMultiLineJList() {
    checkEqual(
        """
          /\\
            1
          /\\
            2
        """,
        "(/\\ 1 2)"
    );
    checkEqual(
        """
          \\/
            1
          \\/
            2
        """,
        "(\\/ 1 2)"
    );
  }

  @Test
  public void testNestedJList() {
    checkEqual(
        """
          /\\ /\\ 1
             /\\ 2
          /\\ 3
        """,
        "(/\\ (/\\ 1 2) 3)"
    );
    checkEqual(
        """
          \\/ \\/ 1
             \\/ 2
          \\/ 3
        """,
        "(\\/ (\\/ 1 2) 3)"
    );
  }

  @Test
  public void testStartOfLineNestedJList() {
    checkEqual(
        """
        /\\ /\\ 1
           /\\ 2
        /\\ 3
        """,
        "(/\\ (/\\ 1 2) 3)"
    );
    checkEqual(
        """
        \\/ \\/ 1
           \\/ 2
        \\/ 3
        """,
        "(\\/ (\\/ 1 2) 3)"
    );
  }

  @Test
  public void testInfixOpJList() {
    checkEqual(
        """
          /\\ 1
            + 2
          /\\ 3
        """,
        "(/\\ (+ 1 2) 3)"
    );
    checkEqual(
        """
          \\/ 1
            + 2
          \\/ 3
        """,
        "(\\/ (+ 1 2) 3)"
    );
  }

  @Test
  public void testInfixOpTerminatedJList() {
    checkEqual(
        """
          /\\ 1
          + 2
          /\\ 3
        """,
        "(/\\ (+ (/\\ 1) 2) 3)"
    );
    checkEqual(
        """
          \\/ 1
          + 2
          \\/ 3
        """,
        "(\\/ (+ (\\/ 1) 2) 3)"
    );
  }

  @Test
  public void testNotAJList() {
    checkEqual(
        """
          1 /\\ 2
        """,
        "(/\\ 1 2)"
    );
    checkEqual(
        """
          1 \\/ 2
        """,
        "(\\/ 1 2)"
    );
  }

  @Test
  public void testJlistWithParentheses() {
    checkEqual(
        """
          /\\ (1)
          /\\ (2)
        """,
        "(/\\ (group 1) (group 2))"
    );
    checkEqual(
        """
          \\/ (1)
          \\/ (2)
        """,
        "(\\/ (group 1) (group 2))"
    );
  }

  @Test
  public void testJlistTerminatedByParentheses() {
    checkEqual(
        """
        ( /\\ 1
           )
          /\\ 2
        """,
        "(/\\ (group (/\\ 1)) 2)"
    );
    checkEqual(
        """
        ( \\/ 1
           )
          \\/ 2
        """,
        "(\\/ (group (\\/ 1)) 2)"
    );
  }

  @Test
  public void testNestedJlistTerminatedByParentheses() {
    checkEqual(
        """
        ( /\\ 1
          /\\  /\\ 2
              /\\ 3
               )
          /\\ 4
        """,
        "(/\\ (group (/\\ 1 (/\\ 2 3))) 4)"
    );
    checkEqual(
        """
        ( \\/ 1
          \\/  \\/ 2
              \\/ 3
               )
          \\/ 4
        """,
        "(\\/ (group (\\/ 1 (\\/ 2 3))) 4)"
    );
  }

  @Test
  public void testDoubleNestedJlistTerminatedByParentheses() {
    checkEqual(
        """
          /\\ 1
          /\\  /\\ 2 + (
                /\\ 3
                /\\ 4
                  )
              /\\ 5
          /\\ 6
        """,
        "(/\\ 1 (/\\ (+ 2 (group (/\\ 3 4))) 5) 6)"
    );
    checkEqual(
        """
          \\/ 1
          \\/  \\/ 2 + (
                \\/ 3
                \\/ 4
                  )
              \\/ 5
          \\/ 6
        """,
        "(\\/ 1 (\\/ (+ 2 (group (\\/ 3 4))) 5) 6)"
    );
  }

  @Test
  public void testConjlistFollowedByDisjunct() {
    checkEqual(
        """
          /\\ 1
          /\\ 2
          \\/ 3
        """,
        "(\\/ (/\\ 1 2) 3)"
    );
  }

  @Test
  public void testDisjlistFollowedByConjunct() {
    checkEqual(
        """
          \\/ 1
          \\/ 2
          /\\ 3
        """,
        "(/\\ (\\/ 1 2) 3)"
    );
  }

  @Test
  public void testNestedAlternatingJlists() {
    checkEqual(
        """
          /\\  \\/ 1
              \\/ 2
          /\\  \\/ 3
              \\/  /\\ 4
                  /\\ 5
          /\\ 6
        """,
        "(/\\ (\\/ 1 2) (\\/ 3 (/\\ 4 5)) 6)"
    );
  }

  @Test
  public void testInvalidParentheses() {
    String input =
        """
        op ==
          /\\ 1
          /\\ (2
        )
          /\\ 3
        """;
    Scanner s = new Scanner(input);
    Parser p = new Parser(s.scanTokens(), false);
    List<Stmt> statements = p.parse();
    for (Stmt statement : statements) {
      assertNull(statement, input);
    }
  }
}

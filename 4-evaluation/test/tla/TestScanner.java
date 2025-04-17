package tla;

import static tla.TokenType.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestScanner {

  private static void compare(List<Token> actual, TokenType... expected) {
    assertEquals(expected.length, actual.size());
    int i = 0;
    for (Token t : actual) {
      assertEquals(expected[i], t.type);
      i++;
    }
  }

  @Test
  public void testEmptyString() {
    Scanner s = new Scanner("");
    compare(s.scanTokens(), EOF);
  }

  @Test
  public void testDelimiters() {
    final String input = "( ) { } [ ] , : == |->";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      LEFT_PAREN, RIGHT_PAREN,
      LEFT_BRACE, RIGHT_BRACE,
      LEFT_BRACKET, RIGHT_BRACKET,
      COMMA, COLON,
      EQUAL_EQUAL, ALL_MAP_TO, EOF
    );
  }

  @Test
  public void testOperators() {
    final String input = "- + < ~ ' = /\\ \\/ ..";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      MINUS, PLUS, LESS_THAN, NOT, PRIME,
      EQUAL, AND, OR, DOT_DOT, EOF
    );
  }

  @Test
  public void testKeywords() {
    final String input =
        "ELSE ENABLED FALSE IF THEN TRUE"
        + " VARIABLE VARIABLES";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      ELSE, ENABLED, FALSE, IF, THEN, TRUE,
      VARIABLES, VARIABLES, EOF
    );
  }
  @Test
  public void testSymbols() {
    final String input =
        "\\land \\E \\exists \\A \\forall"
        + "\\in \\lnot \\neg \\lor";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      AND, EXISTS, EXISTS, FOR_ALL, FOR_ALL,
      IN, NOT, NOT, OR, EOF
    );
  }

  @Test
  public void testNumbers() {
    final String input = "0 12 345 6789";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      NUMBER, NUMBER, NUMBER, NUMBER, EOF
    );
  }

  @Test
  public void testIdentifiers() {
    final String input = "a bc1 def3 ghij4";
    Scanner s = new Scanner(input);
    compare(s.scanTokens(),
      IDENTIFIER, IDENTIFIER,
      IDENTIFIER, IDENTIFIER, EOF
    );
  }
}

package tla;

import static tla.TokenType.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestScanner {

  private static void compare(String input, TokenType... expected) {
    List<Token> actual = Utils.scan(input);
    assertEquals(expected.length, actual.size());
    int i = 0;
    for (Token t : actual) {
      assertEquals(expected[i], t.type);
      i++;
    }
  }

  @Test
  public void testEmptyString() {
    compare("", EOF);
  }

  @Test
  public void testDelimiters() {
    compare(
      "( ) { } [ ] , : == |->",
      LEFT_PAREN, RIGHT_PAREN,
      LEFT_BRACE, RIGHT_BRACE,
      LEFT_BRACKET, RIGHT_BRACKET,
      COMMA, COLON,
      EQUAL_EQUAL, ALL_MAP_TO, EOF
    );
  }

  @Test
  public void testOperators() {
    compare(
      "- + < ~ ' = /\\ \\/ ..",
      MINUS, PLUS, LESS_THAN, NOT, PRIME,
      EQUAL, AND, OR, DOT_DOT, EOF
    );
  }

  @Test
  public void testKeywords() {
    compare(
      "ELSE ENABLED FALSE IF THEN TRUE VARIABLE VARIABLES",
      ELSE, ENABLED, FALSE, IF, THEN, TRUE,
      VARIABLES, VARIABLES, EOF
    );
  }
  @Test
  public void testSymbols() {
    compare(
      "\\land \\E \\exists \\A \\forall \\in \\lnot \\neg \\lor",
      AND, EXISTS, EXISTS, FOR_ALL, FOR_ALL,
      IN, NOT, NOT, OR, EOF
    );
  }

  @Test
  public void testNumbers() {
    compare(
      "0 12 345 6789",
      NUMBER, NUMBER, NUMBER, NUMBER, EOF
    );
  }

  @Test
  public void testIdentifiers() {
    compare(
      "a bc1 def3 ghij4",
      IDENTIFIER, IDENTIFIER,
      IDENTIFIER, IDENTIFIER, EOF
    );
  }
}

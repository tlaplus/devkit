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
  public void testDelimiters() {
    final String input = "( ) { } [ ] , :";
    Scanner s = new Scanner(input);
    List<Token> actual = s.scanTokens();
    compare(s.scanTokens(),
      LEFT_PAREN, RIGHT_PAREN,
      LEFT_BRACE, RIGHT_BRACE,
      LEFT_BRACKET, RIGHT_BRACKET,
      COMMA, COLON, EOF, EOF
    );
  }
}

package tla;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

class Utils {
  static List<Token> scan(IOCapture io, String input) {
    Scanner s = new Scanner(input);
    return s.scanTokens();
  }

  static List<Token> scan(String input) {
    try (IOCapture io = new IOCapture()) {
      return scan(io, input);
    }
  }

  static List<Stmt> parse(IOCapture io, String input) {
    Parser p = new Parser(scan(io, input), true);
    return p.parse();
  }

  static List<Stmt> parse(String input) {
    try (IOCapture io = new IOCapture()) {
      return parse(io, input);
    }
  }

  static String parseToSExpr(String input) {
    AstPrinter p = new AstPrinter();
    List<Stmt> statements = parse(input);
    for (Stmt statement : statements) {
      assertNotNull(statement, input);
    }
    return p.print(statements);
  }

  static boolean hasParseError(String input) {
    try (IOCapture io = new IOCapture()) {
      return parse(io, input).stream().anyMatch(stmt -> null == stmt);
    }
  }

  static String interpret(IOCapture io, String input) {
    Interpreter i = new Interpreter(true);
    i.interpret(parse(io, input));
    return io.getCapturedOutput().out().strip();
  }

  static String interpret(String input) {
    try (IOCapture io = new IOCapture()) {
      return interpret(io, input);
    }
  }

  static boolean hasInterpreterError(String input) {
    try (IOCapture io = new IOCapture()) {
      Utils.interpret(io, input);
      return "" != io.getCapturedOutput().err().strip();
    }
  }
}

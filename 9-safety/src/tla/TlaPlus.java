package tla;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class TlaPlus {
  private static Interpreter interpreter;
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      interpreter = new Interpreter(false);
      runFile(args[0]);
    } else {
      interpreter = new Interpreter(true);
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    String input = new String(bytes, StandardCharsets.UTF_8);
    Scanner scanner = new Scanner(input);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens, false);
    List<Stmt> statements = parser.parse();

    if (hadError) System.exit(65);

    interpreter.interpret(statements);
    
    //runPrompt();

    if (hadRuntimeError) System.exit(70);
    
    ModelChecker mc = new ModelChecker(interpreter, statements);
    try {
      List<Map<String, Object>> trace = mc.checkSafety();
      System.out.println(
          trace == null
          ? "Invariant holds on state space."
          : "Invariant does not hold; trace:\n" + trace
      );
    } catch (RuntimeError e) {
      TlaPlus.runtimeError(e);
      System.exit(70);
    }
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line, true);
      hadError = false;
    }
  }

  private static void run(String source, boolean replMode) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens, replMode);
    List<Stmt> statements = parser.parse();

    // Stop if there was a syntax error.
    if (hadError) return;

    System.out.println(new AstPrinter().print(statements));
    if (replMode && statements.size() == 1
        && statements.get(0) instanceof Stmt.Print) {
      tryStep((Stmt.Print)statements.get(0));
    } else {
      interpreter.interpret(statements);
    }
  }

  private static void tryStep(Stmt.Print action) {
    Object result = interpreter.executeBlock(action.expression, interpreter.globals);
    if (!(result instanceof Boolean)) {
      action.accept(interpreter);
      return;
    }

    List<Map<String, Object>> nextStates =
      interpreter.getNextStates(action.location, action.expression);
    if (nextStates.isEmpty()) {
      action.accept(interpreter);
      return;
    }

    Map<String, Object> nextState = pickNext(nextStates);
    interpreter.goToState(nextState);
    System.out.println(true);
    System.out.println(nextState);
  }

  private static Map<String, Object> pickNext(List<Map<String, Object>> nextStates) {
    if (nextStates.size() == 1) {
      return nextStates.get(0);
    } else {
      System.out.println("Select next state (enter number): ");
      for (int i = 0; i < nextStates.size(); i++) {
        System.out.println(i + ": " + nextStates.get(i));
      }

      System.out.print("> ");
      try (java.util.Scanner in = new java.util.Scanner(System.in)) {
        return nextStates.get(in.nextInt());
      }
    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where,
                             String message) {
    System.err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}


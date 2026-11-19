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
  private static BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in));
  private static Interpreter interpreter;
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: tlaplus [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    interpreter = new Interpreter(false);
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, StandardCharsets.UTF_8), false);

    // Indicate an error in the exit code.
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() throws IOException {
    interpreter = new Interpreter(true);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line, true);
      hadError = false;
    }
  }

  private static void run(String source, boolean replMode) throws IOException {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens, replMode);
    List<Stmt> statements = parser.parse();

    // Stop if there was a syntax error.
    if (hadError) return;

    System.out.println(new AstPrinter().print(statements));
    if (replMode && statements.size() == 1
        && statements.get(0) instanceof Stmt.Print action) {
      try {
        tryStep(action);
      } catch (RuntimeError error) {
        runtimeError(error);
      }
    } else {
      interpreter.interpret(statements);
    }

    if (!replMode) {
      ModelChecker mc = new ModelChecker(interpreter, statements);
      ModelChecker.StateTrace trace = mc.checkSafety();
      System.out.println(
          trace == null
          ? "Invariants hold on state space."
          : "Invariants do not hold; trace:\n" + trace
      );
    }
  }

  private static void tryStep(Stmt.Print action) throws IOException {
    Object result = interpreter.executeBlock(action.expression, interpreter.globals);
    if (!(result instanceof Boolean)) {
      action.accept(interpreter);
      return;
    }

    List<Map<String, Object>> nextStates =
      interpreter.getNextStates(action.location, action.expression, false);
    if (nextStates.isEmpty()) {
      action.accept(interpreter);
      return;
    }

    Map<String, Object> nextState = pickNext(nextStates);
    interpreter.goToState(nextState);
    System.out.println(true);
    System.out.println(nextState);
  }

  private static Map<String, Object> pickNext(List<Map<String, Object>> nextStates) throws IOException {
    if (nextStates.size() == 1) {
      return nextStates.get(0);
    } else {
      System.out.println("Select next state (enter number): ");
      for (int i = 0; i < nextStates.size(); i++) {
        System.out.println(i + ": " + nextStates.get(i));
      }

      System.out.print("> ");
      return nextStates.get(Integer.parseInt(reader.readLine()));
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


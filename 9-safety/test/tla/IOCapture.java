package tla;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

class IOCapture implements AutoCloseable {

  public record RecordedOutput(String out, String err) { }

  private final PrintStream stdout = System.out;
  private final PrintStream stderr = System.err;
  private final InputStream stdin = System.in;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();
  private final PipedOutputStream in = new PipedOutputStream();

  public IOCapture() {
    System.setOut(new PrintStream(out));
    System.setErr(new PrintStream(err));
    try {
      System.setIn(new PipedInputStream(in));
    } catch (IOException e) {
      stderr.println(e);
    }
  }

  public void writeIn(String input) {
    try {
      in.write(input.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      stderr.println(e);
    }
  }

  public RecordedOutput getCapturedOutput() {
    return new RecordedOutput(out.toString(), err.toString());
  }

  @Override
  public void close() {
    System.setOut(stdout);
    System.setErr(stderr);
    System.setIn(stdin);
  }
}

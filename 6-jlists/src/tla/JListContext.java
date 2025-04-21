package tla;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import static tla.TokenType.*;

class JListContext {

  private enum JunctionListType {
    CONJUNCTION,
    DISJUNCTION
  }

  private class JListInfo {
    public final JunctionListType Type;
    public final int Column;
    public JListInfo(JunctionListType type, int column) {
      this.Type = type;
      this.Column = column;
    }
  }

  private final Deque<JListInfo> stack = new ArrayDeque<JListInfo>();

  private static boolean isJListBulletToken(TokenType kind) {
    return AND == kind || OR == kind;
  }

  private static JunctionListType asJListType(TokenType kind) {
    switch (kind) {
      case AND: return JunctionListType.CONJUNCTION;
      case OR: return JunctionListType.DISJUNCTION;
      default: return null; // Unreachable
    }
  }

  public void startNewJList(Token op) {
    this.stack.push(new JListInfo(asJListType(op.type), op.column));
  }

  public void terminateCurrentJList() throws NoSuchElementException {
    this.stack.pop();
  }

  public boolean isNewBullet(Token op) {
    JListInfo headOrNull = this.stack.peekFirst();
    return
      headOrNull != null
      && isJListBulletToken(op.type)
      && headOrNull.Column == op.column
      && headOrNull.Type == asJListType(op.type);
  }

  public boolean isAboveCurrent(Token tok) {
    JListInfo headOrNull = this.stack.peekFirst();
    return headOrNull == null || headOrNull.Column < tok.column;
  }

  public void dump() {
    this.stack.clear();
  }
}

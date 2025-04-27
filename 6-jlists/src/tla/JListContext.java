package tla;

import java.util.ArrayDeque;
import java.util.Deque;

import static tla.TokenType.*;

class JListContext {

  private enum JListType {
    CONJUNCTION,
    DISJUNCTION
  }

  private record JListInfo(JListType type, int column) { }

  private final Deque<JListInfo> stack = new ArrayDeque<JListInfo>();

  private static boolean isJListBulletToken(TokenType kind) {
    return AND == kind || OR == kind;
  }

  private static JListType asJListType(TokenType kind) {
    switch (kind) {
      case AND: return JListType.CONJUNCTION;
      case OR: return JListType.DISJUNCTION;
      default: return null; // Unreachable
    }
  }

  public void startNewJList(Token op) {
    this.stack.push(new JListInfo(asJListType(op.type), op.column));
  }

  public void terminateCurrentJList() {
    this.stack.pop();
  }

  public boolean isNewBullet(Token op) {
    JListInfo headOrNull = this.stack.peekFirst();
    return
      headOrNull != null
      && isJListBulletToken(op.type)
      && headOrNull.column == op.column
      && headOrNull.type == asJListType(op.type);
  }

  public boolean isAboveCurrent(Token tok) {
    JListInfo headOrNull = this.stack.peekFirst();
    return headOrNull == null || headOrNull.column < tok.column;
  }

  public void dump() {
    this.stack.clear();
  }
}

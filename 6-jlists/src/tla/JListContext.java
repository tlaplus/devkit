package tla;

import java.util.ArrayDeque;
import java.util.Deque;

class JListContext {

  private record JListInfo(TokenType type, int column) { }

  private final Deque<JListInfo> stack = new ArrayDeque<>();

  public void startNew(Token op) {
    stack.push(new JListInfo(op.type, op.column));
  }

  public void terminateCurrent() {
    stack.pop();
  }

  public boolean isNewBullet(Token op) {
    JListInfo current = this.stack.peekFirst();
    return current != null
        && current.type == op.type
        && current.column == op.column;
  }

  public boolean isAboveCurrent(Token tok) {
    JListInfo current = this.stack.peekFirst();
    return current == null || current.column < tok.column;
  }

  public void dump() {
    this.stack.clear();
  }
}

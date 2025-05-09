package tla;

record UnboundVariable(Token name, boolean isPrimed) {

  UnboundVariable prime() {
    return new UnboundVariable(name, true);
  }
  
  static UnboundVariable as(Object o) {
    return
      o instanceof UnboundVariable
      ? (UnboundVariable)o
      : null;
  }
  
  @Override
  public String toString() {
    return name.lexeme + (isPrimed ? "'" : "");
  }
}

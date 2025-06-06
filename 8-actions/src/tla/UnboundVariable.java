package tla;

record UnboundVariable(Token name, boolean primed) {
  @Override
  public String toString() {
    return name.lexeme + (primed ? "'" : "");
  }
}

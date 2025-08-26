package tla;

class Level {
  record Constant(Object value) { }
  record State(Expr expr) { }
  record Action(Expr expr) { }
}
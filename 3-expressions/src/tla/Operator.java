package tla;

enum Fix {
  PREFIX, INFIX, POSTFIX
}

class Operator {
  final Fix fix;
  final TokenType token;
  final boolean assoc;
  final int lowPrec;
  final int highPrec;

  public Operator(Fix fix, TokenType token, boolean assoc,
                  int lowPrec, int highPrec) {
    this.fix = fix;
    this.token = token;
    this.assoc = assoc;
    this.lowPrec = lowPrec;
    this.highPrec = highPrec;
  }
}

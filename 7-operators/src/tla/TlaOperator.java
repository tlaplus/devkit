package tla;

import java.util.ArrayList;
import java.util.List;

class TlaOperator implements TlaCallable {
  private final Token name;
  private final List<Token> params;
  private final Expr body;
  TlaOperator(Stmt.OpDef declaration) {
    this.name = declaration.name;
    this.params = declaration.params;
    this.body = declaration.body;
  }
  
  TlaOperator(Expr expression) {
    name = null;
    params = new ArrayList<>();
    body = expression;
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
    Environment environment = new Environment(interpreter.globals);
    for (int i = 0; i < params.size(); i++) {
      environment.define(params.get(i), arguments.get(i));
    }

    return interpreter.execute(body, environment);
  }
  
  @Override
  public String toString() {
    return "<fn " + name.lexeme + ">";
  }
}

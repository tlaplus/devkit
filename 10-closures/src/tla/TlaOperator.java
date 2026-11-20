package tla;

import java.util.List;

class TlaOperator implements TlaCallable {
  private final Stmt.OpDef declaration;
  private final Environment closure;

  TlaOperator(Stmt.OpDef declaration, Environment closure) {
    this.closure = closure;
    this.declaration = declaration;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i), arguments.get(i));
    }

    return interpreter.executeBlock(declaration.body, environment);
  }
  
  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}

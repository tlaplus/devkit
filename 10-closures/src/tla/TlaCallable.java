package tla;

import java.util.List;

interface TlaCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);
}

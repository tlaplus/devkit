package tla;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class BindingGenerator implements Iterator<Environment>,
                                  Iterable<Environment> {
  private final List<Token> vars;
  private final List<Object> set;
  private final Environment parent;
  private int enumerationIndex = 0;

  BindingGenerator(List<Token> vars, Set<?> set, Environment parent) {
    this.vars = vars;
    this.set = new ArrayList<>(set);
    this.parent = parent;
  }

  @Override
  public boolean hasNext() {
    return enumerationIndex < Math.pow(set.size(), vars.size());
  }

  @Override
  public Environment next() {
    int current = enumerationIndex++;
    Environment bindings = new Environment(parent);
    for (Token var : vars) {
      bindings.define(var, set.get(current % set.size()));
      current /= set.size();
    }

    return bindings;
  }

  @Override
  public Iterator<Environment> iterator() {
    return this;
  }
}

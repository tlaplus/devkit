package com.craftinginterpreters.tla;

import java.util.Set;
import java.util.HashSet;

import com.craftinginterpreters.tla.Expr.Binary;
import com.craftinginterpreters.tla.Expr.Grouping;
import com.craftinginterpreters.tla.Expr.Literal;
import com.craftinginterpreters.tla.Expr.Ternary;
import com.craftinginterpreters.tla.Expr.Unary;
import com.craftinginterpreters.tla.Expr.Variadic;

class Interpreter implements Expr.Visitor<Object> {
  
  void interpret(Expr expression) {
    try {
      Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      TlaPlus.runtimeError(error);
    }
  }
  
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  @Override
  public Object visitBinaryExpr(Binary expr) {
    Object left = evaluate(expr.left);
    switch (expr.operator.type) {
      case AND:
        checkBooleanOperand(expr.operator, left);
        if (!(boolean)left) return false;
        Object right = evaluate(expr.right);
        checkBooleanOperand(expr.operator, right);
        return (boolean)right;
      default:
        break;
    }
    
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (int)left - (int)right;
      case PLUS:
        checkNumberOperands(expr.operator, left, right);
        return (int)left + (int)right;
      case LESS_THAN:
        checkNumberOperands(expr.operator, left, right);
        return (int)left < (int)right;
      case EQUAL:
        if (left instanceof Integer && right instanceof Integer) {
          return (int)left == (int)right;
        }
        if (left instanceof Boolean && right instanceof Boolean) {
          return (boolean)left == (boolean)right;
        }
        if (left instanceof Set<?> && right instanceof Set<?>) {
          return setEquals((Set<?>)left, (Set<?>)right);
        }
      case DOT_DOT:
        checkNumberOperands(expr.operator, left, right);
        Set<Object> set = new HashSet<Object>();
        int lower = (int)left;
        int higher = (int)right;
        if (lower < higher) {
          for (int i = lower; i <= higher; i++) {
            set.add(i);
          }
        }
        return set;
      case OR:
        checkBooleanOperands(expr.operator, left, right);
        return (boolean)left || (boolean)right;
      case IN:
        checkSetOperand(expr.operator, right);
        return setContains(left, (Set<?>)right);
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitGroupingExpr(Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitUnaryExpr(Unary expr) {
    Object operand = evaluate(expr.expr);
    
    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, operand);
        return -(int)operand;
      case ENABLED:
        checkBooleanOperand(expr.operator, operand);
        return (boolean)operand;
      case NEGATION:
        checkBooleanOperand(expr.operator, operand);
        return !(boolean)operand;
      case PRIME:
        return operand;
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitTernaryExpr(Ternary expr) {
    switch (expr.operator.type) {
      case IF:
        Object conditional = evaluate(expr.first);
        checkBooleanOperand(expr.operator, conditional);
        return (boolean)conditional ?
            evaluate(expr.second) : evaluate(expr.third); 
      default:
        // Unreachable.
        return null;
    }
  }

  @Override
  public Object visitVariadicExpr(Variadic expr) {
    switch (expr.operator.type) {
      case LEFT_BRACE:
        Set<Object> set = new HashSet<Object>();
        for (Expr parameter : expr.parameters) {
          set.add(evaluate(parameter));
        }
        return set;
      default:
        // Unreachable.
        return null;
    }
  }
  
  private String stringify(Object object) {
    return object.toString();
  }

  private static boolean setContains(Object candidate, Set<?> set) {
    return set.stream().anyMatch((Object element) -> {
      if (candidate instanceof Integer && element instanceof Integer) {
        return (int)candidate == (int)element;
      }
      if (candidate instanceof Boolean && element instanceof Boolean) {
        return (boolean)candidate == (boolean)element;
      }
      if (candidate instanceof Set<?> && element instanceof Set<?>) {
        return setEquals((Set<?>)candidate, (Set<?>)element);
      }
      return false;
    });
  }
  
  private static boolean setEquals(Set<?> left, Set<?> right) {
    if (left.size() != right.size()) {
      return false;
    }
    
    return left.stream().allMatch(
        (Object element) -> setContains(element, right));
  }
  
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Integer) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator,
      Object left, Object right) {
    if (left instanceof Integer && right instanceof Integer) return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }
  
  private void checkBooleanOperand(Token operator, Object operand) {
    if (operand instanceof Boolean) return;
    throw new RuntimeError(operator, "Operand must be a boolean.");
  }

  private void checkBooleanOperands(Token operator, Object left, Object right) {
    if (left instanceof Boolean && right instanceof Boolean) return;
    throw new RuntimeError(operator, "Operands must be booleans.");
  }
  
  private void checkSetOperand(Token operator, Object operand) {
    if (operand instanceof Set<?>) return;
    throw new RuntimeError(operator, "Operand must be a set.");
  }
}

package com.richanna.math;

/**
 * Provides arithmetic operations on a type.
 */
public interface Computable<T extends Number> extends Comparable<Computable<T>>, Cloneable {
  Computable<T> clone() throws CloneNotSupportedException;
  Numbers.Converter<T> getConverter();

  T getValue();

  Computable<T> dividedBy(Computable<T> x);
  Computable<T> dividedBy(T x);
  Computable<T> minus(Computable<T> x);
  Computable<T> minus(T x);
  Computable<T> plus(Computable<T> x);
  Computable<T> plus(T x);
  Computable<T> times(Computable<T> x);
  Computable<T> times(T x);
}

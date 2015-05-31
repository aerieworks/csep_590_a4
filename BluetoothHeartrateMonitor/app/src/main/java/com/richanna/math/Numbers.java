package com.richanna.math;

import org.apache.commons.lang3.builder.CompareToBuilder;

public final class Numbers {

  private Numbers() {}

  public static interface Converter<T extends Number> {
    Computable<T> to(int value);
    Computable<T> to(T value);
    Computable<T> zero();
  }

  protected static abstract class MathableBase<T extends Number> implements Computable<T> {
    private T value;

    protected MathableBase(final T value) {
      setValue(value);
    }

    private void setValue(final T value) {
      this.value = value;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public Computable<T> clone() throws CloneNotSupportedException {
      final MathableBase<T> other = (MathableBase<T>)super.clone();
      other.setValue(this.getValue());
      return other;
    }

    @Override
    public int compareTo(final Computable<T> other) {
      if (other == null) {
        return 1;
      }
      return new CompareToBuilder()
          .append(getValue(), other.getValue())
          .toComparison();
    }

    @Override
    public Computable<T> dividedBy(final Computable<T> x) {
      return dividedBy(x.getValue());
    }

    @Override
    public Computable<T> dividedBy(final T x) {
      if (getValue() == null || x == null) {
        setValue(null);
      } else {
        setValue(divide(getValue(), x));
      }

      return this;
    }

    @Override
    public Computable<T> minus(final Computable<T> x) {
      return minus(x.getValue());
    }

    @Override
    public Computable<T> minus(final T x) {
      if (getValue() == null || x == null) {
        setValue(null);
      } else {
        setValue(subtract(getValue(), x));
      }

      return this;
    }

    @Override
    public Computable<T> plus(final Computable<T> x) {
      return plus(x.getValue());
    }

    @Override
    public Computable<T> plus(final T x) {
      if (getValue() == null || x == null) {
        setValue(null);
      } else {
        setValue(add(getValue(), x));
      }

      return this;
    }

    @Override
    public Computable<T> times(final Computable<T> x) {
      return times(x.getValue());
    }

    @Override
    public Computable<T> times(final T x) {
      if (getValue() == null || x == null) {
        setValue(null);
      } else {
        setValue(multiply(getValue(), x));
      }

      return this;
    }

    protected abstract T add(final T lhs, final T rhs);
    protected abstract T divide(final T lhs, final T rhs);
    protected abstract T multiply(final T lhs, final T rhs);
    protected abstract T subtract(final T lhs, final T rhs);
  }

  public static class I extends MathableBase<Long> {

    public I() {
      this(0L);
    }

    public I(final Long value) {
      super(value);
    }

    @Override
    protected Long add(Long lhs, Long rhs) {
      return lhs + rhs;
    }

    @Override
    protected Long divide(Long lhs, Long rhs) {
      return lhs / rhs;
    }

    @Override
    protected Long subtract(Long lhs, Long rhs) {
      return lhs - rhs;
    }

    @Override
    protected Long multiply(Long lhs, Long rhs) {
      return lhs * rhs;
    }

    @Override
    public Converter<Long> getConverter() {
      return converter;
    }

    public static final Converter<Long> converter = new Numbers.Converter<Long>() {

      @Override
      public Computable<Long> to(final int value) {
        return new I((long)value);
      }

      @Override
      public Computable<Long> to(final Long value) {
        return new I(value);
      }

      @Override
      public Computable<Long> zero() {
        return new I();
      }
    };
  }

  public static class R extends MathableBase<Double> {

    public R() {
      this(0.0);
    }

    public R(final Double value) {
      super(value);
    }

    @Override
    protected Double add(Double lhs, Double rhs) {
      return lhs + rhs;
    }

    @Override
    protected Double divide(Double lhs, Double rhs) {
      return lhs / rhs;
    }

    @Override
    protected Double multiply(Double lhs, Double rhs) {
      return lhs * rhs;
    }

    @Override
    protected Double subtract(Double lhs, Double rhs) {
      return lhs - rhs;
    }

    @Override
    public Converter<Double> getConverter() {
      return converter;
    }

    public static final Converter<Double> converter = new Numbers.Converter<Double>() {

      @Override
      public Computable<Double> to(final int value) {
        return new R((double)value);
      }

      @Override
      public Computable<Double> to(final Double value) {
        return new R(value);
      }

      @Override
      public Computable<Double> zero() {
        return new R();
      }
    };
  }
}

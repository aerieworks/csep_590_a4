package com.richanna.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DataPoint<T> {
  private final long timestamp;
  private final T value;

  public DataPoint(final long timestamp, final T value) {
    this.timestamp = timestamp;
    this.value = value;
  }

  public long getTimestamp() { return timestamp; }
  public T getValue() { return value; }

  @Override
  public String toString() {
    final String valueText = getValue() == null ? "<null>" : getValue().toString();
    return String.format("%s @ %d", valueText, getTimestamp());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    } else if (o == null || o.getClass() != this.getClass()) {
      return false;
    }

    final DataPoint<T> other = (DataPoint<T>)o;
    return new EqualsBuilder()
        .append(this.getTimestamp(), other.getTimestamp())
        .append(this.getValue(), other.getValue())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(this.getTimestamp())
        .append(this.getValue())
        .toHashCode();
  }
}

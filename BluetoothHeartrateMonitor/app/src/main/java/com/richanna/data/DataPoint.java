package com.richanna.data;

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
}

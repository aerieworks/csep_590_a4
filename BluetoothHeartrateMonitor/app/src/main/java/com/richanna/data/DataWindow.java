package com.richanna.data;

import java.util.Collection;

public class DataWindow<T> {

  private final long startTime;
  private final long endTime;
  private final Collection<T> data;

  public DataWindow(final long startTime, final long endTime, final Collection<T> data) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.data = data;
  }

  public Collection<T> getData() { return data; }
  public int getSize() { return data.size(); }
  public long getStartTime() { return startTime; }
  public long getEndTime() { return endTime; }
}

package com.richanna.events;

public interface Listener<T> {
  public void tell(final T eventData);
}

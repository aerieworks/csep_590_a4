package com.richanna.events;

import java.util.HashSet;
import java.util.Set;

public class Event<T> {

  private final Set<Listener<T>> listeners = new HashSet<>();
  public final Listenable listenable = new Listenable();

  public void fire(final T eventData) {
    for (final Listener<T> listener : listeners) {
      listener.tell(eventData);
    }
  }

  public class Listenable {

    public void listen(final Listener<T> listener) {
      Event.this.listeners.add(listener);
    }

    public void clearListeners() {
      Event.this.listeners.clear();
    }
  }
}

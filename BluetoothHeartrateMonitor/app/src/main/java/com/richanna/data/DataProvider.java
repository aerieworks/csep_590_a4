package com.richanna.data;

import com.richanna.events.Listener;

public interface DataProvider<T> {
  public void addOnNewDatumListener(final Listener<T> listener);
  public void clearListeners();
}

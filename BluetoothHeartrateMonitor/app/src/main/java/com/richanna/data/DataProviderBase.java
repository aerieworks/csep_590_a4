package com.richanna.data;

import android.util.Log;

import com.richanna.events.Event;
import com.richanna.events.Listener;

public class DataProviderBase<T> implements DataProvider<T> {

  private static final String TAG = "DataProviderBase";

  private final Event<DataPoint<T>> newDatumEvent = new Event<>();

  protected void provideDatum(final DataPoint<T> datum) {
    newDatumEvent.fire(datum);
  }

  protected  void provideDatum(final T datum) {
    newDatumEvent.fire(new DataPoint<>(System.nanoTime(), datum));
  }

  @Override
  public void addOnNewDatumListener(final Listener<DataPoint<T>> listener) {
    Log.i(TAG, String.format("%s is now listening to %s", listener.getClass().getSimpleName(), this.getClass().getSimpleName()));
    newDatumEvent.listenable.listen(listener);
  }

  @Override
  public void clearListeners() {
    newDatumEvent.listenable.clearListeners();
  }

  @Override
  public void reset() {}
}

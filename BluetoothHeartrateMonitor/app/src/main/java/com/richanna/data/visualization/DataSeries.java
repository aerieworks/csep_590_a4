package com.richanna.data.visualization;

import android.util.Pair;

import com.androidplot.xy.XYSeries;
import com.richanna.events.Event;

public abstract class DataSeries implements XYSeries {

  private final String title;
  private final int formatterId;
  protected final Event<DataSeries> seriesUpdatedEvent = new Event<>();
  public final Event<DataSeries>.Listenable onSeriesUpdated = seriesUpdatedEvent.listenable;

  protected DataSeries(final String title, final int formatterId) {
    this.title = title;
    this.formatterId = formatterId;
  }

  protected abstract Pair<Number, Number> getDataPoint(final int index);
  protected abstract int getSize();

  public int getFormatterId() { return formatterId; }

  @Override
  public Number getX(int index) {
    if (getSize() > index || index == 0) {
      return index;
    }

    return null;
  }

  @Override
  public Number getY(int index) {
    if (getSize() > index) {
      return getDataPoint(index).second;
    }

    return index == 0 ? 0 : null;
  }

  @Override
  public int size() {
    final int size = getSize();
    return size > 0 ? size : 1;
  }

  @Override
  public String getTitle() {
    return title;
  }
}

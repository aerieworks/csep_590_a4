package com.richanna.data.visualization;

import android.util.Pair;

import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.events.Listener;

import java.util.ArrayList;
import java.util.List;

public class StreamingSeries extends DataSeries implements Listener<DataPoint<Float>> {

  private final int maxSize;
  private final List<Pair<Number, Number>> series;
  private int index = 0;

  public StreamingSeries(final DataProvider<DataPoint<Float>> source, final String title, final int formatterId, final int maxSize) {
    super(title, formatterId);
    this.maxSize = maxSize;
    series = new ArrayList<>(maxSize);
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(final DataPoint<Float> dataPoint) {
    if (series.size() == maxSize) {
      series.remove(0);
    }

    series.add(new Pair<Number, Number>(index, dataPoint.getValue()));
    index += 1;
    seriesUpdatedEvent.fire(this);
  }

  @Override
  protected int getSize() {
    return series.size();
  }

  @Override
  protected Pair<Number, Number> getDataPoint(int index) {
    return series.get(index);
  }
}

package com.richanna.data.visualization;

import android.util.Pair;

import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataWindow;
import com.richanna.events.Listener;

import java.util.ArrayList;
import java.util.List;

public class WindowedSeries extends DataSeries implements Listener<DataWindow<DataPoint<Float>>> {

  private List<Pair<Number, Number>> currentWindow;

  public WindowedSeries(final DataProvider<DataWindow<DataPoint<Float>>> source, final String title, final int formatterId) {
    super(title, formatterId);
    this.currentWindow = new ArrayList<>();
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(final DataWindow<DataPoint<Float>> window) {
    currentWindow.clear();
    for (final DataPoint<Float> dataPoint : window.getData()) {
      currentWindow.add(new Pair<Number, Number>(currentWindow.size(), dataPoint.getValue()));
    }

    seriesUpdatedEvent.fire(this);
  }

  @Override
  protected int getSize() {
    return currentWindow.size();
  }

  @Override
  protected Pair<Number, Number> getDataPoint(int index) {
    return currentWindow.get(index);
  }
}

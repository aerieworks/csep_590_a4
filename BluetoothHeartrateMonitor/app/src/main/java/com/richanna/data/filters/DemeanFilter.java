package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

import java.util.ArrayList;
import java.util.List;

public class DemeanFilter extends DataProviderBase<DataPoint<Float>> implements DataFilter<DataPoint<Float>, DataPoint<Float>> {

  private static final String TAG = "DemeanFilter";

  private final int windowSize;
  private final List<DataPoint<Float>> window;
  private boolean hasMean = false;
  private float windowTotal = 0;

  public DemeanFilter(final int windowSize, final DataProvider<DataPoint<Float>> source) {
    this.windowSize = windowSize;
    window = new ArrayList<>(windowSize);
    source.addOnNewDatumListener(this);

    Log.i(TAG, String.format("Window size: %d", this.windowSize));
  }

  @Override
  public void tell(DataPoint<Float> dataPoint) {
    while (window.size() >= windowSize) {
      windowTotal -= window.get(0).getValue();
      window.remove(0);
    }

    window.add(dataPoint);
    windowTotal += dataPoint.getValue();

    if (window.size() == windowSize) {
      final float mean = windowTotal / (float) windowSize;
      if (!hasMean) {
        hasMean = true;
        for (int i = 0; i < windowSize / 2; i++) {
          provideDatum(i, mean);
        }
      }

      provideDatum(windowSize / 2, mean);
    }
  }

  private void provideDatum(final int sourceIndex, final float mean) {
    final DataPoint<Float> source = window.get(sourceIndex);
    provideDatum(new DataPoint<>(source.getTimestamp(), source.getValue() - mean));
  }
}

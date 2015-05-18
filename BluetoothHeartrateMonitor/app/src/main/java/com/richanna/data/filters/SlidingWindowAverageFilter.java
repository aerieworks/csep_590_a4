package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowAverageFilter extends DataProviderBase<DataPoint<Long>> implements DataFilter<DataPoint<Long>, DataPoint<Long>> {
  private static final String TAG = "SWAFilter";

  private final int windowSize;
  private final List<Long> window = new ArrayList<>();
  private long sum = 0;

  public SlidingWindowAverageFilter(final int windowSize, final DataProvider<DataPoint<Long>> source) {
    this.windowSize = windowSize;
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(DataPoint<Long> eventData) {
    final long value = eventData.getValue();
    window.add(value);
    sum += value;

    while (window.size() > windowSize) {
      sum -= window.remove(0);
    }

    Log.d(TAG, String.format("Sliding window: %d, %d", sum, sum / window.size()));
    final DataPoint<Long> dataPoint = new DataPoint<>(eventData.getTimestamp(), sum / window.size());
    provideDatum(dataPoint);
  }
}

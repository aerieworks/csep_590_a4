package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

/**
 * Generates data points whose values are the interval in nanoseconds between data points provided by the source data provider.
 */
public class TimeIntervalFilter<TSource> extends DataProviderBase<Long> implements DataFilter<TSource, Long> {

  private static final String TAG = "TimeIntervalFilter";

  private Long lastDataPointTimestamp = null;

  public TimeIntervalFilter(final DataProvider<TSource> source) {
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(DataPoint<TSource> eventData) {
    final long currentDataPointTimestamp = eventData.getTimestamp();
    Long interval = null;
    if (lastDataPointTimestamp != null) {
      interval = currentDataPointTimestamp - lastDataPointTimestamp;
    }

    lastDataPointTimestamp = currentDataPointTimestamp;
    if (interval != null) {
      Log.i(TAG, String.format("Time interval: %d", interval));
      provideDatum(new DataPoint<>(currentDataPointTimestamp, interval));
    }
  }
}

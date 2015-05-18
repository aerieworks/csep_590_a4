package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

public class ZeroCrossingFinder extends DataProviderBase<DataPoint<Long>> implements DataFilter<DataPoint<Float>, DataPoint<Long>> {
  private static final String TAG = "ZeroCrossingFinder";

  private final float positiveThreshold;
  private boolean isPositive = false;
  private long lastCrossing = 0;

  public ZeroCrossingFinder(final float positiveThreshold, final DataProvider<DataPoint<Float>> source) {
    this.positiveThreshold = positiveThreshold;
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(DataPoint<Float> eventData) {
    final float value = eventData.getValue();

    if (!isPositive) {
      isPositive = (value > positiveThreshold);
    } else if (value < 0) {
      isPositive = false;
      final long timestamp = eventData.getTimestamp();
      if (lastCrossing > 0) {
        final DataPoint<Long> dataPoint = new DataPoint<>(timestamp, (timestamp - lastCrossing) / 1000000l);
        Log.d(TAG, String.format("Found zero crossing: %d", dataPoint.getValue()));
        provideDatum(dataPoint);
      }

      lastCrossing = timestamp;
    }
  }
}

package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

public class ZeroCrossingFilter extends DataProviderBase<Float> implements DataFilter<Float, Float> {
  private static final String TAG = "ZeroCrossingFinder";

  private final float positiveThreshold;
  private final float[] thresholdWindow;
  private int thresholdWindowIndex = 0;
  private float thresholdPeak = 0;
  private boolean isPositive = false;

  public ZeroCrossingFilter(final float positiveThreshold, final int thresholdWindowSize, final DataProvider<Float> source) {
    this.positiveThreshold = positiveThreshold;
    thresholdWindow = new float[thresholdWindowSize];
    for (int i = 0; i < thresholdWindowSize; i++) {
      thresholdWindow[i] = 0;
    }

    source.addOnNewDatumListener(this);
  }

  @Override
  public void reset() {
    for (int i = 0; i < thresholdWindow.length; i++) {
      thresholdWindow[i] = 0;
    }
    thresholdPeak = 0;
    thresholdWindowIndex = 0;
    isPositive = false;
  }

  @Override
  public void tell(DataPoint<Float> eventData) {
    final float value = eventData.getValue();

    // Adjust the peak value if necessary.
    if (value >= thresholdPeak) {
      thresholdPeak = value;
      Log.i(TAG, String.format("Saw new peak: %f", thresholdPeak));
    } else if (thresholdWindow[thresholdWindowIndex] == thresholdPeak) {
      // The previous peak is falling out of the window, so recalculate.
      Log.i(TAG, String.format("Previous peak fell out of window: %f", thresholdPeak));
      thresholdPeak = 0;
      for (int i = 1; i < thresholdWindow.length; i++) {
        final float current = thresholdWindow[i % thresholdWindow.length];
        if (current > thresholdPeak) {
          thresholdPeak = current;
        }
      }
      Log.i(TAG, String.format("Found new peak: %f", thresholdPeak));
    }
    thresholdWindow[thresholdWindowIndex] = value;
    thresholdWindowIndex = (thresholdWindowIndex + 1) % thresholdWindow.length;

    if (!isPositive) {
      isPositive = (value > thresholdPeak * positiveThreshold);
      if (isPositive) {
        Log.i(TAG, String.format("Positive crossing: %f > %f", value, thresholdPeak * positiveThreshold));
      }
    } else if (value < 0) {
      isPositive = false;
      Log.i(TAG, String.format("Found zero crossing: %f", eventData.getValue()));
      provideDatum(eventData);
    }
  }
}

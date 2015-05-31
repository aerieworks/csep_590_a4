package com.richanna.data.filters;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds data points representing peaks in the source data point stream.
 */
public class PeakFilter extends DataProviderBase<Float> implements DataFilter<Float, Float> {

  private static final String TAG = "PeakFilter";

  private final int windowSize;
  private final float peakThreshold;
  private final List<DataPoint<Float>> window;
  private DataPoint<Float> peakCandidate;
  private float windowSum;
  private long lastPeak;

  public PeakFilter(final int windowSize, final float peakThreshold, final DataProvider<Float> source) {
    this.windowSize = windowSize;
    this.window = new ArrayList<>(windowSize);
    this.peakThreshold = 1.0F + peakThreshold;

    reset();

    source.addOnNewDatumListener(this);
  }

  @Override
  public void reset() {
    window.clear();
    peakCandidate = null;
    windowSum = 0;
    lastPeak = 0;
  }

  @Override
  public void tell(DataPoint<Float> eventData) {
    if (eventData.getValue() == null) {
      return;
    }

    final float value = eventData.getValue();

    if (value > 0) {
      while (window.size() >= windowSize) {
        final DataPoint<Float> ejected = window.remove(0);
        windowSum -= ejected.getValue();
      }
      window.add(eventData);
      windowSum += value;
    }

    if (lastPeak == 0) {
      lastPeak = eventData.getTimestamp();
    }

    final float average = windowSum / (float)window.size();
    if (value > peakThreshold * average) {
      Log.d(TAG, String.format("May have peaked: %f @ %d", value, eventData.getTimestamp()));
      peakCandidate = eventData;
    } else if (peakCandidate != null && value < 0) {
      final DataPoint<Float> peak = peakCandidate;
      Log.i(TAG, String.format("Peak: %f -> %f, %f, %d", peak.getValue(), value, average, (peak.getTimestamp() - lastPeak) / 1000000L));
      lastPeak = peak.getTimestamp();
      peakCandidate = null;
      provideDatum(peak);
    }
  }
}

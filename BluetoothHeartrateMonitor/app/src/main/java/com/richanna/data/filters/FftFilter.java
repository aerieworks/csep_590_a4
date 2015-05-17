package com.richanna.data.filters;

import android.util.Log;

import com.badlogic.gdx.audio.analysis.FFT;
import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;
import com.richanna.data.DataWindow;

import java.util.ArrayList;
import java.util.List;

public class FftFilter extends DataProviderBase<DataWindow<DataPoint<Float>>> implements DataFilter<DataPoint<Float>, DataWindow<DataPoint<Float>>> {

  private static final String TAG = "FftFilter";

  private final List<DataPoint<Float>> dataPoints = new ArrayList<>();
  private final int windowSize;

  public FftFilter(final int windowSize, final DataProvider<DataPoint<Float>> source) {
    this.windowSize = windowSize;
    source.addOnNewDatumListener(this);

    Log.i(TAG, String.format("Window size: %d", this.windowSize));
  }

  @Override
  public void tell(DataPoint<Float> eventData) {
    dataPoints.add(eventData);
    if (dataPoints.size() == windowSize) {
      computeFft();
      while (dataPoints.size() > windowSize * 0.75) {
        dataPoints.remove(0);
      }
    }
  }

  private void computeFft() {
    final long timestamp = System.nanoTime();
    final float[] samples = new float[windowSize];
    for (int i = 0; i < windowSize; i++) {
      samples[i] = dataPoints.get(i).getValue();
    }

    final FFT fft = new FFT(windowSize, 18);
    fft.forward(samples);

    fft.getSpectrum();
    final float[] real = fft.getRealPart();
    final float[] imaginary = fft.getImaginaryPart();

    final float[] magnitude = new float[windowSize / 2];
    final List<DataPoint<Float>> newPoints = new ArrayList<>(magnitude.length);
    for (int i = 0; i < magnitude.length; i++) {
      magnitude[i] = (float)Math.sqrt((real[i] * real[i]) + (imaginary[i] * imaginary[i]));
      newPoints.add(new DataPoint<>(timestamp, magnitude[i]));
    }
    provideDatum(new DataWindow<>(dataPoints.get(0).getTimestamp(), dataPoints.get(dataPoints.size() - 1).getTimestamp(), newPoints));
  }
}

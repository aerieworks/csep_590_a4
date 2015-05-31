package com.richanna.data.filters;

import android.util.Log;

import com.richanna.math.Computable;
import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;
import com.richanna.math.Numbers;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowAverageFilter<T extends Number> extends DataProviderBase<T> implements DataFilter<T, T> {

  private static int counter = 0;
  private final String TAG = String.format("SWAFilter[%d]", counter++);

  private final int windowSize;
  private final List<T> window = new ArrayList<>();
  private final Numbers.Converter<T> converter;
  private Computable<T> sum;

  public SlidingWindowAverageFilter(final int windowSize, final Numbers.Converter<T> converter, final DataProvider<T> source) {
    this.windowSize = windowSize;
    this.converter = converter;

    reset();

    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(DataPoint <T> eventData) {
    while (window.size() >= windowSize) {
      sum.minus(window.remove(0));
    }

    final T value = eventData.getValue();
    window.add(value);
    sum.plus(value);

    if (window.size() == windowSize) {
      final T average = converter.to(sum.getValue())
          .dividedBy(converter.to(window.size()))
          .getValue();
      Log.i(TAG, "New sliding window");
      final DataPoint<T> dataPoint = new DataPoint<>(eventData.getTimestamp(), average);
      provideDatum(dataPoint);
    }
  }

  @Override
  public void reset() {
    Log.i(TAG, "Resetting");
    sum = converter.zero();
    window.clear();
  }
}

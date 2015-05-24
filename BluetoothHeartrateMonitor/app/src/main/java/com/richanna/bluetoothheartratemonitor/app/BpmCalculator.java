package com.richanna.bluetoothheartratemonitor.app;

import android.util.Log;

import com.richanna.data.DataFilter;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;

public class BpmCalculator extends DataProviderBase<DataPoint<Long>> implements DataFilter<DataPoint<Long>, DataPoint<Long>> {

  private static final String TAG = "BpmCalculator";
  private static final long MIN_BPM = 30;
  private static final long MAX_BPM = 240;

  private Long previousBpm = null;

  public BpmCalculator(final DataProvider<DataPoint<Long>> source) {
    source.addOnNewDatumListener(this);
  }

  @Override
  public void tell(DataPoint<Long> eventData) {
    Long bpm = 60000 / eventData.getValue();
    Log.d(TAG, String.format("BPM: %d", bpm));
    if (bpm < MIN_BPM || bpm > MAX_BPM) {
      bpm = null;
    }
    if ((bpm == null && previousBpm != null) || (bpm != null && !bpm.equals(previousBpm))) {
      Log.i(TAG, String.format("BPM changed from %d to %d.", previousBpm == null ? 0 : previousBpm, bpm == null ? 0 : bpm));
      previousBpm = bpm;
      final DataPoint<Long> dataPoint = new DataPoint<>(System.nanoTime(), bpm);
      provideDatum(dataPoint);
    }
  }
}

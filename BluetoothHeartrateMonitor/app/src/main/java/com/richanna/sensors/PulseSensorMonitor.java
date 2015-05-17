package com.richanna.sensors;

import android.bluetooth.BluetoothGattCharacteristic;

import com.richanna.data.DataGenerator;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProviderBase;

/**
 * Created by annabelle on 5/16/15.
 */
public class PulseSensorMonitor extends DataProviderBase<DataPoint<Integer>> implements DataGenerator<DataPoint<Integer>> {

  private BluetoothGattCharacteristic readCharacteristic;

  public PulseSensorMonitor() {

  }


  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }
}

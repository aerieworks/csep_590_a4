package com.richanna.bluetoothheartratemonitor.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by annabelle on 5/14/15.
 */
public class MonitorService extends Service {

  private final IBinder binder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  private class LocalBinder extends Binder {

  }
}

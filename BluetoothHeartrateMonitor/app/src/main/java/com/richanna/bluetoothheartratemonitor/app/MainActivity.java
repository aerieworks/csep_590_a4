package com.richanna.bluetoothheartratemonitor.app;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.androidplot.ui.TextOrientationType;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.richanna.bluetoothheartratemonitor.R;
import com.richanna.bluetoothheartratemonitor.service.MonitorService;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.data.DataProviderBase;
import com.richanna.data.filters.SlidingWindowAverageFilter;
import com.richanna.data.filters.ZeroCrossingFinder;
import com.richanna.data.visualization.DataSeries;
import com.richanna.data.visualization.StreamingSeries;
import com.richanna.events.Listener;


public class MainActivity extends ActionBarActivity {
  private static final String TAG = "MainActivity";

  private static final int REQUEST_ENABLE_BLUETOOTH = 1;

  private BluetoothAdapter bluetoothAdapter;
  private BroadcastReceiver monitorServiceReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      switch (action) {
        case MonitorService.ACTION_BLUETOOTH_DISABLED:
          promptToEnableBluetooth();
          break;
        case MonitorService.ACTION_DATA_AVAILABLE:
          pulseSensor.readSensorValue(intent);
          break;
      }
    }
  };

  private TextView lblHeartRate;
  private XYPlot sensorPlot;
  private PulseSensorMonitor pulseSensor = new PulseSensorMonitor();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    lblHeartRate = (TextView)findViewById(R.id.lblHeartRate);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      Log.i(TAG, "BT: Device does not support Bluetooth. :(");
    }

    final DataProvider<DataPoint<Long>> bpmProvider =
        new SlidingWindowAverageFilter(10,
            new ZeroCrossingFinder(50, pulseSensor)
        );
    bpmProvider.addOnNewDatumListener(new Listener<DataPoint<Long>>() {
      @Override
      public void tell(final DataPoint<Long> eventData) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            final long bpm = 60000 / eventData.getValue();
            Log.d(TAG, String.format("BPM: %d", bpm));
            if (bpm < 30 || bpm > 240) {
              lblHeartRate.setText(getResources().getString(R.string.default_heart_rate_value));
            } else {
              lblHeartRate.setText(Long.toString(bpm));
            }
          }
        });
      }
    });

    sensorPlot = initializePlot(R.id.sensorPlot);
    final StreamingSeries sensorSeries = new StreamingSeries(pulseSensor, "Pulse Sensor", R.xml.line_formatting_sensor_plot, getResources().getInteger(R.integer.max_points_sensor_plot));
    addSeriesToPlot(sensorPlot, sensorSeries);
    sensorSeries.onSeriesUpdated.listen(new Listener<DataSeries>() {
      @Override
      public void tell(DataSeries eventData) {
        sensorPlot.redraw();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerReceiver(monitorServiceReceiver, MonitorService.getIntentFilter());
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "Starting monitor svc from resume.");
    startMonitorService();
  }

  @Override
  protected void onStop() {
    super.onStop();
    unregisterReceiver(monitorServiceReceiver);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    final Intent intent = new Intent(this, MonitorService.class);
    stopService(intent);
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
      case REQUEST_ENABLE_BLUETOOTH:
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Bluetooth enabled, starting service.");
          startMonitorService();
        } else {
          Log.i(TAG, "Bluetooth not enabled, cannot connect to device.");
        }
        break;
      default:
        Log.w(TAG, String.format("Unrecognized request code in onActivityResult: %d", requestCode));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void startMonitorService() {
    final Intent intent = new Intent(this, MonitorService.class);
    startService(intent);
  }

  private XYPlot initializePlot(final int plotId) {
    final XYPlot plot = (XYPlot) findViewById(plotId);
    plot.centerOnRangeOrigin(0);
    plot.setTicksPerRangeLabel(3);
    plot.getGraphWidget().setDomainLabelPaint(null);
    plot.getGraphWidget().setDomainOriginLabelPaint(null);
    plot.getLayoutManager().remove(plot.getLegendWidget());
    plot.getLayoutManager().remove(plot.getTitleWidget());
    plot.getLayoutManager().remove(plot.getDomainLabelWidget());
    plot.getRangeLabelWidget().setOrientation(TextOrientationType.HORIZONTAL);
    return plot;
  }

  private void addSeriesToPlot(final XYPlot plot, final DataSeries series) {
    final LineAndPointFormatter formatter = new LineAndPointFormatter();
    formatter.configure(this, series.getFormatterId());
    plot.addSeries(series, formatter);
  }

  private void promptToEnableBluetooth() {
    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
  }

  public void btnConnect_onClick(View view) {
    if (!bluetoothAdapter.isEnabled()) {
      promptToEnableBluetooth();
    } else {
      startMonitorService();
    }
  }

  private static class PulseSensorMonitor extends DataProviderBase<DataPoint<Float>> {
    public void readSensorValue(final Intent intent) {
      final float value = intent.getFloatExtra(MonitorService.KEY_SENSOR_VALUE, 0);
      final DataPoint<Float> dataPoint = new DataPoint<>(System.nanoTime(), value);
      provideDatum(dataPoint);
    }
  }
}

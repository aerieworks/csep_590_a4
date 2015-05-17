package com.richanna.bluetoothheartratemonitor.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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
import com.richanna.data.DataPoint;
import com.richanna.data.DataProviderBase;
import com.richanna.data.DataWindow;
import com.richanna.data.filters.DemeanFilter;
import com.richanna.data.filters.FftFilter;
import com.richanna.data.visualization.DataSeries;
import com.richanna.data.visualization.StreamingSeries;
import com.richanna.data.visualization.WindowedSeries;
import com.richanna.events.Listener;

import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
  private static final String TAG = "MainActivity";

  private static final String DEVICE_NAME = "richanna<3mon";
  private static final UUID UUID_SERVICE = UUID.fromString("00002220-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_RECEIVE = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

  private static final int REQUEST_ENABLE_BLUETOOTH = 1;

  private static final int FFT_WINDOW_SIZE = 256;
  private static final float MAX_HEART_RATE_HZ = 4.0f;

  private BluetoothAdapter bluetoothAdapter;
  private BluetoothGatt bluetoothGatt;
  private BluetoothGattCharacteristic readCharacteristic;
  private final BluetoothGattCallback gattCallback = new MonitorGattCallback();

  private TextView lblHeartRate;
  private XYPlot sensorPlot;
  private XYPlot fftPlot;
  private PulseSensorMonitor pulseSensor = new PulseSensorMonitor();
  private DemeanFilter demeanFilter;
  private FftFilter fftFilter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    lblHeartRate = (TextView)findViewById(R.id.lblHeartRate);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      Log.i(TAG, "BT: Device does not support Bluetooth. :(");
    }

    sensorPlot = initializePlot(R.id.sensorPlot);
    fftPlot = initializePlot(R.id.fftPlot);

    demeanFilter = new DemeanFilter(50, pulseSensor);
    fftFilter = new FftFilter(FFT_WINDOW_SIZE, demeanFilter);
    fftFilter.addOnNewDatumListener(new Listener<DataWindow<DataPoint<Float>>>() {
      @Override
      public void tell(final DataWindow<DataPoint<Float>> eventData) {
        final float timespan = (float) (eventData.getEndTime() - eventData.getStartTime()) / 1000000000f;
        final float sampleRate = (float) FFT_WINDOW_SIZE / timespan;
        final float rateStep = (sampleRate / 2.0f) / (float) eventData.getSize();

        Log.d("MonitorActivity", String.format("Sample rate: %f", sampleRate));
        int maxIndex = 0;
        float maxValue = 0;
        int index = 0;
        for (final DataPoint<Float> dataPoint : eventData.getData()) {
          if (index * rateStep > MAX_HEART_RATE_HZ) {
            break;
          }

          if (dataPoint.getValue() > maxValue) {
            maxIndex = index;
            maxValue = dataPoint.getValue();
          }

          index += 1;
        }

        final float frequency = (float) maxIndex * rateStep;
        final int bpm = (int) (frequency * 60.0f);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            lblHeartRate.setText(Integer.toString(bpm));
            fftPlot.redraw();
          }
        });
      }
    });

    final StreamingSeries sensorSeries = new StreamingSeries(demeanFilter, "Pulse Sensor", R.xml.line_formatting_sensor_plot, getResources().getInteger(R.integer.max_points_sensor_plot));
    sensorSeries.onSeriesUpdated.listen(new Listener<DataSeries>() {
      @Override
      public void tell(DataSeries eventData) {
        sensorPlot.redraw();
      }
    });

    addSeriesToPlot(sensorPlot, sensorSeries);
    addSeriesToPlot(fftPlot, new WindowedSeries(fftFilter, "FFT", R.xml.line_formatting_sensor_plot));
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (bluetoothGatt != null) {
      connectToDevice();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (bluetoothGatt != null) {
      bluetoothGatt.disconnect();
    }
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
      case REQUEST_ENABLE_BLUETOOTH:
        connectToDevice();
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

  public void btnConnect_onClick(View view) {
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
    } else {
      connectToDevice();
    }
  }

  private void connectToDevice() {
    if (bluetoothGatt == null) {
      final Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
      Log.d(TAG, String.format("BT: Found %d paired devices.", devices.size()));
      for (final BluetoothDevice device : devices) {
        Log.d(TAG, String.format("%s: %s", device.getName(), device.getAddress()));
        if (DEVICE_NAME.equals(device.getName())) {
          Log.d(TAG, "Attempting to connect to monitor...");
          bluetoothGatt = device.connectGatt(this, false, gattCallback);
        }
      }
    } else {
      Log.d(TAG, "Attempting to resume connection to monitor...");
      if (bluetoothGatt.connect()) {
        Log.d(TAG, "Connection resumed.");
      } else {
        Log.d(TAG, "Failed to resume connection.");
        try {
          bluetoothGatt.close();
        } finally {
          bluetoothGatt = null;
        }
      }
    }
  }

  private class MonitorGattCallback extends BluetoothGattCallback {
    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
      if (newState == BluetoothGatt.STATE_CONNECTED) {
        Log.d(TAG, "Discovering services...");
        gatt.discoverServices();
      } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
        Log.d(TAG, "Disconnected.");
        try {
          gatt.disconnect();
          gatt.close();
        } finally {
          MainActivity.this.bluetoothGatt = null;
        }
      }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "Found service, setting up read notifications...");
        final BluetoothGattService service = gatt.getService(UUID_SERVICE);
        MainActivity.this.readCharacteristic = service.getCharacteristic(UUID_RECEIVE);
        if (MainActivity.this.readCharacteristic != null) {
          final BluetoothGattDescriptor descriptor = MainActivity.this.readCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
          if (descriptor != null) {
            gatt.setCharacteristicNotification(MainActivity.this.readCharacteristic, true);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            Log.d(TAG, "Starting to read.");
            gatt.readCharacteristic(MainActivity.this.readCharacteristic);
          } else {
            Log.w(TAG, "Client Configuration descriptor not found.");
          }
        } else {
          Log.w(TAG, "Read characteristic not found.");
        }
      }
    }

    @Override
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, String.format("Got characteristic read: %s", characteristic.getUuid()));
        pulseSensor.readBluetoothValue(characteristic);
      }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
      Log.d(TAG, String.format("Characteristic changed: %s", characteristic.getUuid()));
      pulseSensor.readBluetoothValue(characteristic);
    }
  }

  private static class PulseSensorMonitor extends DataProviderBase<DataPoint<Float>> {

    public void readBluetoothValue(final BluetoothGattCharacteristic characteristic) {
      final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
      Log.d(TAG, String.format("Input length: %d; value: %d", characteristic.getValue().length, value));
      final DataPoint<Float> dataPoint = new DataPoint<>(System.nanoTime(), (float)value);
      provideDatum(dataPoint);
    }
  }
}

package com.richanna.bluetoothheartratemonitor.app;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidplot.ui.TextOrientationType;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.richanna.bluetoothheartratemonitor.R;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProvider;
import com.richanna.math.Numbers;
import com.richanna.data.filters.PeakFilter;
import com.richanna.data.filters.SlidingWindowAverageFilter;
import com.richanna.data.filters.TimeIntervalFilter;
import com.richanna.data.visualization.DataSeries;
import com.richanna.data.visualization.StreamingSeries;
import com.richanna.events.Listener;

import org.apache.commons.lang3.StringUtils;


public class MainActivity extends ActionBarActivity {
  private static final String TAG = "MainActivity";

  public static final int REQUEST_ENABLE_BLUETOOTH = 1;
  public static final int REQUEST_SELECT_MONITOR = 2;

  private final MonitorDataSource.StatusCallback monitorStatusCallback =  new MonitorDataSource.StatusCallback() {
    @Override
    public void onStatusChange(int status) {
      setStatus(status);
    }
  };

  private final Listener<DataPoint<Long>> bpmListener = new Listener<DataPoint<Long>>() {
    @Override
    public void tell(final DataPoint<Long> eventData) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          final Long bpm = eventData.getValue();
          if (bpm == null) {
            lblHeartRate.setText(getResources().getString(R.string.default_heart_rate_value));
            setStatus(R.string.status_detecting_heart_rate);
          } else {
            lblHeartRate.setText(Long.toString(bpm));
            setStatus(R.string.status_ok);
          }
        }
      });
    }
  };

  private TextView lblHeartRate;
  private RelativeLayout pnlStatus;
  private TextView lblStatus;
  private Button btnEnable;
  private Button btnConnect;
  private Button btnSelect;
  private XYPlot sensorPlot;

  private MonitorDataSource monitorDataSource;
  private DataProvider<Float> wavelengthDetector;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    lblHeartRate = (TextView)findViewById(R.id.lblHeartRate);
    pnlStatus = (RelativeLayout)findViewById(R.id.pnlStatus);
    lblStatus = (TextView)findViewById(R.id.lblStatus);
    btnEnable = (Button)findViewById(R.id.btnEnable);
    btnConnect = (Button)findViewById(R.id.btnConnect);
    btnSelect = (Button)findViewById(R.id.btnSelect);

    monitorDataSource = new MonitorDataSource(this, monitorStatusCallback);
    wavelengthDetector = new PeakFilter(20, 0.5f, monitorDataSource);

    final DataProvider<Long> bpmCalculator = new BpmCalculator(
        new SlidingWindowAverageFilter<>(10, Numbers.I.converter,
          new TimeIntervalFilter<>(wavelengthDetector)
        )
    );
    bpmCalculator.addOnNewDatumListener(bpmListener);

    sensorPlot = initializeSensorPlot();
    final StreamingSeries sensorSeries = new StreamingSeries(monitorDataSource, "Pulse Sensor", R.xml.line_formatting_sensor_plot, getResources().getInteger(R.integer.max_points_sensor_plot));
    addSeriesToPlot(sensorPlot, sensorSeries);
    sensorSeries.onSeriesUpdated.listen(new Listener<DataSeries>() {
      @Override
      public void tell(DataSeries eventData) {
        sensorPlot.redraw();
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    monitorDataSource.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "Starting monitor svc from resume.");
    monitorDataSource.resume();
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    switch (requestCode) {
      case REQUEST_ENABLE_BLUETOOTH:
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Bluetooth enabled, starting service.");
          monitorDataSource.resume();
        } else {
          Log.i(TAG, "Bluetooth not enabled, cannot connect to device.");
        }
        break;
      case REQUEST_SELECT_MONITOR:
        if (resultCode == RESULT_OK) {
          final String deviceName = data.getStringExtra(getString(R.string.selected_monitor_device_name));
          final String deviceAddress = data.getStringExtra(getString(R.string.selected_monitor_device_address));
          Log.i(TAG, String.format("Monitor selected: %s, %s.  Will try to connect.", deviceName, deviceAddress));

          final SharedPreferences preferences = getSharedPreferences(getString(R.string.pref_file_device_preferences), MODE_PRIVATE);
          final SharedPreferences.Editor editor = preferences.edit();
          editor.putString(getString(R.string.pref_device_name), deviceName);
          editor.putString(getString(R.string.pref_device_address), deviceAddress);
          editor.commit();

          monitorDataSource.resume();
        } else {
          Log.i(TAG, "Monitor not selected.");
        }
        break;
      default:
        Log.w(TAG, String.format("Unrecognized request code in onActivityResult: %d", requestCode));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_monitor_info) {
      showMonitorInfo();
      return true;
    } else if (id == R.id.action_select_monitor) {
      selectMonitorDevice();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void setStatus(final int statusId) {
    if (statusId == R.string.status_detecting_heart_rate) {
      wavelengthDetector.reset();
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        pnlStatus.setVisibility(statusId == R.string.status_ok ? View.GONE : View.VISIBLE);
        lblStatus.setText(getResources().getString(statusId));

        btnEnable.setVisibility(statusId == R.string.status_bluetooth_disabled ? View.VISIBLE : View.GONE);
        btnConnect.setVisibility(statusId == R.string.status_monitor_not_found ? View.VISIBLE : View.GONE);
        btnSelect.setVisibility(statusId == R.string.status_monitor_not_selected ? View.VISIBLE : View.GONE);
      }
    });
  }

  private XYPlot initializeSensorPlot() {
    final XYPlot plot = (XYPlot) findViewById(R.id.sensorPlot);
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

  private void showMonitorInfo() {
    final int messageId;

    final SharedPreferences preferences = getDevicePreferences();
    final String deviceName = preferences.getString(getString(R.string.pref_device_name), null);
    final String deviceAddress = preferences.getString(getString(R.string.pref_device_address), null);
    if (StringUtils.isBlank(deviceName) || StringUtils.isBlank(deviceAddress)) {
      messageId = R.string.monitor_info_none_selected;
    } else {
      messageId = R.string.monitor_info_name_and_address;
    }

    new AlertDialog.Builder(this)
        .setTitle(R.string.title_monitor_info)
        .setMessage(getString(messageId, deviceName, deviceAddress))
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private void selectMonitorDevice() {
    final SharedPreferences.Editor editor = getDevicePreferences().edit();
    editor.remove(getString(R.string.pref_device_name));
    editor.remove(getString(R.string.pref_device_address));
    editor.commit();

    Intent selectMonitor = new Intent(this, MonitorSelectActivity.class);
    startActivityForResult(selectMonitor, REQUEST_SELECT_MONITOR);
  }

  private SharedPreferences getDevicePreferences() {
    return getSharedPreferences(getString(R.string.pref_file_device_preferences), Context.MODE_PRIVATE);
  }

  public void btnEnable_onClick(View view) {
    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
  }

  public void btnConnect_onClick(View view) {
    monitorDataSource.resume();
  }

  public void btnSelect_onClick(View view) {
    selectMonitorDevice();
  }
}

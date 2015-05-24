package com.richanna.bluetoothheartratemonitor.app;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
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
import com.richanna.data.filters.SlidingWindowAverageFilter;
import com.richanna.data.filters.ZeroCrossingFinder;
import com.richanna.data.visualization.DataSeries;
import com.richanna.data.visualization.StreamingSeries;
import com.richanna.events.Listener;


public class MainActivity extends ActionBarActivity {
  private static final String TAG = "MainActivity";

  private static final int REQUEST_ENABLE_BLUETOOTH = 1;

  private TextView lblHeartRate;
  private RelativeLayout pnlStatus;
  private TextView lblStatus;
  private Button btnEnable;
  private Button btnConnect;
  private XYPlot sensorPlot;

  private MonitorDataSource monitorDataSource;
  private ZeroCrossingFinder zeroCrossingFinder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    lblHeartRate = (TextView)findViewById(R.id.lblHeartRate);
    pnlStatus = (RelativeLayout)findViewById(R.id.pnlStatus);
    lblStatus = (TextView)findViewById(R.id.lblStatus);
    btnEnable = (Button)findViewById(R.id.btnEnable);
    btnConnect = (Button)findViewById(R.id.btnConnect);

    monitorDataSource = new MonitorDataSource(this, new MonitorDataSource.StatusCallback() {
      @Override
      public void onStatusChange(int status) {
        setStatus(status);
      }
    });
    zeroCrossingFinder = new ZeroCrossingFinder(0.5f, 200, monitorDataSource);

    final DataProvider<DataPoint<Long>> bpmCalculator = new BpmCalculator(
        new SlidingWindowAverageFilter(10, zeroCrossingFinder)
    );
    bpmCalculator.addOnNewDatumListener(new Listener<DataPoint<Long>>() {
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
    });

    sensorPlot = initializePlot(R.id.sensorPlot);
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

  private void setStatus(final int statusId) {
    if (statusId == R.string.status_detecting_heart_rate) {
      zeroCrossingFinder.reset();
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        pnlStatus.setVisibility(statusId == R.string.status_ok ? View.GONE : View.VISIBLE);
        lblStatus.setText(getResources().getString(statusId));

        btnEnable.setVisibility(statusId == R.string.status_bluetooth_disabled ? View.VISIBLE : View.GONE);
        btnConnect.setVisibility(statusId == R.string.status_monitor_disconnected ? View.VISIBLE : View.GONE);
      }
    });
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

  public void btnEnable_onClick(View view) {
    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBluetooth, REQUEST_ENABLE_BLUETOOTH);
  }

  public void btnConnect_onClick(View view) {
    monitorDataSource.resume();
  }
}

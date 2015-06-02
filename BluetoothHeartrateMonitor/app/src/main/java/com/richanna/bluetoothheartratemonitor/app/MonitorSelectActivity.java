package com.richanna.bluetoothheartratemonitor.app;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.richanna.bluetoothheartratemonitor.R;

import java.util.ArrayList;
import java.util.List;

public class MonitorSelectActivity extends ListActivity {

  private static final String TAG = "MonitorSelectActivity";
  private static final int SCAN_TIMEOUT = 10000;
  private static final int PROGRESS_STEP = 100;

  private final BleDeviceListAdapter deviceListAdapter = new BleDeviceListAdapter();
  private BluetoothAdapter bluetoothAdapter;
  private ProgressBar scanProgress;

  private final CountDownTimer scanTimer = new CountDownTimer(SCAN_TIMEOUT, PROGRESS_STEP) {
    @Override
    public void onTick(long millisUntilFinished) {
      scanProgress.setProgress(SCAN_TIMEOUT - (int)millisUntilFinished);
    }

    @Override
    public void onFinish() {
      Log.i(TAG, "Monitor scan timed out.");
      stopScanning();
      if (deviceListAdapter.getCount() == 0) {
        reportNoDevicesFound();
      }
    }
  };

  private final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Log.i(TAG, String.format("Found BLE device: %s, %s", device.getName(), device.getAddress()));
          deviceListAdapter.addDevice(device);
        }
      });
    }
  };

  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);
    setContentView(R.layout.activity_monitor_select);

    scanProgress = (ProgressBar)findViewById(R.id.scanProgress);
    scanProgress.setMax(SCAN_TIMEOUT);

    final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothAdapter = manager.getAdapter();

    setListAdapter(deviceListAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    startScanning();
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopScanning();
  }

  private void startScanning() {
    Log.i(TAG, "Starting to scan for monitor...");
    bluetoothAdapter.startLeScan(scanCallback);
    scanProgress.setProgress(0);
    scanTimer.start();
  }

  private void stopScanning() {
    Log.i(TAG, "Stopping monitor scan...");
    bluetoothAdapter.stopLeScan(scanCallback);
  }

  private void reportNoDevicesFound() {
    new AlertDialog.Builder(this)
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Log.i(TAG, "No devices found, not scanning again.");
            setResult(RESULT_CANCELED, getIntent());
            finish();
          }
        })
      .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          startScanning();
        }
      })
      .setMessage(R.string.select_monitor_scan_again_prompt)
      .show();
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    final BluetoothDevice device = deviceListAdapter.getDevice(position);
    Log.i(TAG, String.format("Device selected: %s, %s", device.getName(), device.getAddress()));
    final Intent intent = getIntent();
    intent.putExtra(getString(R.string.selected_monitor_device_name), device.getName());
    intent.putExtra(getString(R.string.selected_monitor_device_address), device.getAddress());
    setResult(RESULT_OK, intent);
    finish();
  }

  /*@Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_monitor_select, menu);
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
  }*/

  private class BleDeviceListAdapter extends BaseAdapter {

    private final List<BluetoothDevice> devices = new ArrayList<>();

    public void addDevice(final BluetoothDevice device) {
      devices.add(device);
      notifyDataSetChanged();
    }

    public BluetoothDevice getDevice(final int position) {
      return devices.get(position);
    }

    @Override
    public int getCount() {
      return devices.size();
    }

    @Override
    public Object getItem(int position) {
      return getDevice(position);
    }

    @Override
    public long getItemId(int position) {
      return getDevice(position).getAddress().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final BluetoothDevice device = getDevice(position);
      if (device == null) {
        Log.e(TAG, String.format("Device at position %d is null.", position));
      } else {
        Log.i(TAG, String.format("Getting view for device %d: %s, %s", position, device.getName(), device.getAddress()));
      }

      final LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View row = inflater.inflate(android.R.layout.two_line_list_item, parent, false);
      ((TextView)row.findViewById(android.R.id.text1)).setText(device.getName());
      ((TextView)row.findViewById(android.R.id.text2)).setText(device.getAddress());
      return row;
    }
  }
}

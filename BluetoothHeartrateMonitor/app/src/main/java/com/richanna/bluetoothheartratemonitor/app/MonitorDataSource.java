package com.richanna.bluetoothheartratemonitor.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.richanna.bluetoothheartratemonitor.R;
import com.richanna.data.DataGenerator;
import com.richanna.data.DataPoint;
import com.richanna.data.DataProviderBase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

public class MonitorDataSource extends DataProviderBase<Float> implements DataGenerator<Float> {

  private static final String TAG = "MonitorDataSource";

  private static final String DEVICE_NAME = "richanna<3mon";
  private static final UUID UUID_SERVICE = UUID.fromString("00002220-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_RECEIVE = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

  private final GattClient gattClient = new GattClient();
  private final Context context;
  private final StatusCallback statusCallback;
  private BluetoothAdapter adapter;
  private BluetoothGatt gattConnection;

  public MonitorDataSource(final Context context, final StatusCallback statusCallback) {
    this.context = context;
    this.statusCallback = statusCallback;
  }

  @Override
  public void pause() {
    disconnect();
  }

  @Override
  public void resume() {
    connect();
  }

  private void updateStatus(final int status) {
    statusCallback.onStatusChange(status);
  }

  private void connect() {
    if (gattConnection != null) {
      Log.i(TAG, "connect() called; already listening.");
      return;
    } else if (adapter == null) {
      final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      adapter = manager.getAdapter();
      if (adapter == null) {
        Log.i(TAG, "connect() called, but device does not support Bluetooth. :(");
        updateStatus(R.string.status_bluetooth_not_supported);
        return;
      }
    } else if (!adapter.isEnabled()) {
      Log.i(TAG, "connect() called, but Bluetooth is not enabled. :(");
      updateStatus(R.string.status_bluetooth_disabled);
      return;
    }

    final Set<BluetoothDevice> devices = adapter.getBondedDevices();
    for (final BluetoothDevice device : devices) {
      if (DEVICE_NAME.equals(device.getName())) {
        Log.i(TAG, "Attempting to connect to monitor...");
        gattConnection = device.connectGatt(context, false, gattClient);
        updateStatus(R.string.status_waiting_for_monitor);
        return;
      }
    }

    Log.i(TAG, "Monitor has not been paired to device.");
    updateStatus(R.string.status_monitor_disconnected);
  }

  private void disconnect() {
    Log.i(TAG, "Disconnecting from monitor...");
    cleanupConnection();
  }

  private void cleanupConnection() {
    if (gattConnection != null) {
      gattConnection.disconnect();
      gattConnection.close();
      gattConnection = null;

      if (adapter.isEnabled()) {
        updateStatus(R.string.status_monitor_disconnected);
      } else {
        updateStatus(R.string.status_bluetooth_disabled);
      }
    }
  }

  private class GattClient extends BluetoothGattCallback {
    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
      if (newState == BluetoothGatt.STATE_CONNECTED) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          Log.i(TAG, "Connected to monitor, discovering services...");
          gattConnection.discoverServices();
        } else {
          Log.i(TAG, "Failed to connect to monitor.  Is it powered up/in range?");
          cleanupConnection();
        }
      } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
        Log.i(TAG, "Disconnected from monitor.");
        cleanupConnection();
      }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.i(TAG, "Found service, starting to listen for data from monitor...");
        final BluetoothGattService service = gatt.getService(UUID_SERVICE);
        final BluetoothGattCharacteristic readCharacteristic = service.getCharacteristic(UUID_RECEIVE);
        if (readCharacteristic != null) {
          final BluetoothGattDescriptor descriptor = readCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
          if (descriptor != null) {
            gatt.setCharacteristicNotification(readCharacteristic, true);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            updateStatus(R.string.status_detecting_heart_rate);
          } else {
            Log.e(TAG, "Client Configuration descriptor not found on monitor GATT service.");
          }
        } else {
          Log.e(TAG, "Read characteristic not found on monitor GATT service.");
        }
      }
    }

    @Override
    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
      final int dataLength = characteristic.getValue().length;
      final ByteBuffer buffer = ByteBuffer.wrap(characteristic.getValue(), 0, dataLength);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      final DataPoint<Float> dataPoint = new DataPoint<>(System.nanoTime(), buffer.getFloat());
      //Log.i(TAG, String.format("Received value: %f @ %d", dataPoint.getValue(), dataPoint.getTimestamp()));
      provideDatum(dataPoint);
    }
  }

  public static interface StatusCallback {
    void onStatusChange(final int status);
  }
}
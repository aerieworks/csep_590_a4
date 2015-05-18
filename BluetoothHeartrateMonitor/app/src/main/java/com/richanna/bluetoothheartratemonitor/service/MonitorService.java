package com.richanna.bluetoothheartratemonitor.service;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

public class MonitorService extends Service {

  private static final String TAG = "MonitorService";

  private static final String DEVICE_NAME = "richanna<3mon";

  private static final UUID UUID_SERVICE = UUID.fromString("00002220-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_RECEIVE = UUID.fromString("00002221-0000-1000-8000-00805F9B34FB");
  private static final UUID UUID_CLIENT_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

  public static final String ACTION_CONNECTED = "com.richanna.bluetoothheartratemonitor.service.ACTION_CONNECTED";
  public static final String ACTION_DISCONNECTED = "com.richanna.bluetoothheartratemonitor.service.ACTION_DISCONNECTED";
  public static final String ACTION_DATA_AVAILABLE = "com.richanna.bluetoothheartratemonitor.service.ACTION_DATA_AVAILABLE";
  public static final String ACTION_BLUETOOTH_DISABLED = "com.richanna.bluetoothheartratemonitor.service.ACTION_BLUETOOTH_DISABLED";

  public static final String KEY_SENSOR_VALUE = "com.richanna.bluetoothheartratemonitor.service.KEY_SENSOR_VALUE";

  private BluetoothAdapter adapter;
  private MonitorGattClient gattClient;


  @Override
  public void onCreate() {
    Log.i(TAG, "Service created.");
    final BluetoothManager manager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
    adapter = manager.getAdapter();
    if (adapter == null) {
      Log.i(TAG, "Device does not support Bluetooth. :(");
      stopSelf();
    }

    gattClient = new MonitorGattClient(this, adapter);
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    Log.i(TAG, "Service start requested.");
    if (adapter != null) {
      if (adapter.isEnabled()) {
        gattClient.connect();
      } else {
        signalClient(ACTION_BLUETOOTH_DISABLED);
      }
    }
    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    if (gattClient != null) {
      gattClient.disconnect();
    }
  }

  private void signalClient(final String action) {
    final Intent intent = new Intent(action);
    signalClient(intent);
  }

  private void signalClient(final Intent intent) {
    sendBroadcast(intent, Manifest.permission.BLUETOOTH);
  }

  public static IntentFilter getIntentFilter() {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_CONNECTED);
    filter.addAction(ACTION_DISCONNECTED);
    filter.addAction(ACTION_DATA_AVAILABLE);
    filter.addAction(ACTION_BLUETOOTH_DISABLED);
    return filter;
  }

  private class MonitorGattClient extends BluetoothGattCallback {

    private final Context context;
    private final BluetoothAdapter adapter;
    private BluetoothGatt gattConnection;
    private boolean isConnected = false;

    public MonitorGattClient(final Context context, final BluetoothAdapter adapter) {
      this.context = context;
      this.adapter = adapter;
    }

    public boolean connect() {
      if (isConnected) {
        gattConnection.discoverServices();
        return true;
      }

      if (gattConnection == null) {
        final Set<BluetoothDevice> devices = adapter.getBondedDevices();
        for (final BluetoothDevice device : devices) {
          if (DEVICE_NAME.equals(device.getName())) {
            Log.i(TAG, "Attempting to connect to monitor...");
            gattConnection = device.connectGatt(context, false, this);
            return true;
          }
        }

        Log.i(TAG, "Monitor has not been paired to device.");
      } else {
        Log.i(TAG, "Attempting to resume connection to monitor...");
        if (gattConnection.connect()) {
          Log.i(TAG, "Resumed connection to monitor.");
          return true;
        }

        Log.i(TAG, "Failed to resume connection to monitor.");
        cleanupConnection();
      }

      return false;
    }

    public void disconnect() {
      Log.i(TAG, "Disconnecting from monitor...");
      cleanupConnection();
    }

    private void cleanupConnection() {
      isConnected = false;
      if (gattConnection != null) {
        gattConnection.disconnect();
        gattConnection.close();
      }
    }

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
      if (newState == BluetoothGatt.STATE_CONNECTED) {
        isConnected = true;
        Log.i(TAG, "Connected to monitor, discovering services...");
        gattConnection.discoverServices();
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
      final float value = buffer.getFloat();
      final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
      intent.putExtra(KEY_SENSOR_VALUE, value);
      signalClient(intent);
    }
  }
}

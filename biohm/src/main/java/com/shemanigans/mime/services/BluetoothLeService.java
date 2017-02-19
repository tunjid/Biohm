package com.shemanigans.mime.services;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.shemanigans.mime.R;
import com.shemanigans.mime.SampleGattAttributes;
import com.shemanigans.mime.activities.MainActivity;
import com.shemanigans.mime.activities.OverviewActivity;
import com.shemanigans.mime.models.DeviceData;
import com.shemanigans.mime.models.DeviceStatus;
import com.shemanigans.mime.models.RXpair;
import com.shemanigans.mime.models.TaubinSolution;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service
        implements Observer<TaubinSolution> {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private boolean isBound = false;

    // private static final int GATT_INDETERMINATE = 8;
    public static final int ONGOING_NOTIFICATION_ID = 2;
    public static final int HISTORY_SIZE = 270;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    // BioImpedance data parsing variables

    public final static String DEVICE_NAME = "DEVICE_NAME";
    public final static String DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public final static String GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String GATT_CONNECTING = "ACTION_GATT_CONNECTING";
    public final static String GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String GATT_SERVICES_DISCOVERED = "GATT_SERVICES_DISCOVERED";

    public final static String DATA_AVAILABLE_SAMPLE_RATE = "DATA_AVAILABLE_SAMPLE_RATE";
    public final static String DATA_AVAILABLE_BIOIMPEDANCE = "DATA_AVAILABLE_BIOIMPEDANCE";
    public final static String DATA_AVAILABLE_TAUBIN_SOLUTION = "DATA_AVAILABLE_TAUBIN_SOLUTION";
    public final static String DATA_AVAILABLE_FREQUENCY_PARAMS = "DATA_AVAILABLE_FREQUENCY_PARAMS";

    public final static String DATA_AVAILABLE_UNKNOWN = "ACTION_DATA_AVAILABLE";

    public final static String EXTRA_DATA = "EXTRA_DATA";

    private boolean isInitialized;

    private String deviceName;
    private String deviceAddress;

    private String connectionState = GATT_DISCONNECTED;

    private BluetoothGattCharacteristic impedanceCharacteristic;
    private BluetoothGattCharacteristic sampleRateCharacteristic;
    private BluetoothGattCharacteristic ACFrequencyCharacteristic;

    // Queue for reading multiple characteristics due to delay induced by callback.
    private Queue<BluetoothGattCharacteristic> readQueue = new LinkedList<>();
    private ArrayList<RXpair> curvePoints = new ArrayList<>();

    public static DeviceStatus deviceStatus = new DeviceStatus();

    private final IBinder mBinder = new LocalBinder();

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    connectionState = GATT_CONNECTED;
                    bluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    connectionState = GATT_CONNECTING;
                    broadcastUpdate(connectionState);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    connectionState = GATT_DISCONNECTED;


                    if (!isBound) { // Not bound anymore, update notification
                        stopForeground(true);

                        Intent scanIntent = new Intent(BluetoothLeService.this, MainActivity.class);
                        scanIntent.putExtra(MainActivity.GO_TO_SCAN, true);
                        scanIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        final PendingIntent activityPendingIntent = PendingIntent.getActivity(
                                BluetoothLeService.this, 0, scanIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(BluetoothLeService.this)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle(getString(R.string.disconnected))
                                .setContentText(getText(R.string.disconnected_from_device))
                                .setContentIntent(activityPendingIntent);

                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        notificationManager.notify(ONGOING_NOTIFICATION_ID, notificationBuilder.build());
                    }

                    broadcastUpdate(connectionState);
                    break;
            }

            Log.i(TAG, "STATE = " + connectionState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            boolean success = status == BluetoothGatt.GATT_SUCCESS;

            if (success) {
                broadcastUpdate(GATT_SERVICES_DISCOVERED);
                findGattServices(getSupportedGattServices());
                broadcastUpdate(connectionState);
            }

            Log.i(TAG, "onServicesDiscovered staus: " + success);
        }

        @Override
        // Checks queue for characteristics to be read and reads them
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {

                // Remove the characterictic that was just read
                if (readQueue.size() > 0) readQueue.remove();

                switch (characteristic.getUuid().toString()) {
                    case SampleGattAttributes.SAMPLE_RATE:
                        broadcastUpdate(DATA_AVAILABLE_SAMPLE_RATE, characteristic);
                        break;
                    case SampleGattAttributes.AC_FREQ:
                        broadcastUpdate(DATA_AVAILABLE_FREQUENCY_PARAMS, characteristic);
                        break;
                    default:
                        broadcastUpdate(DATA_AVAILABLE_UNKNOWN, characteristic);
                        break;
                }
            }
            else {
                showToast(BluetoothLeService.this, "onCharacteristicRead error: " + status);
            }

            // Read the top of the queue
            if (readQueue.size() > 0) bluetoothGatt.readCharacteristic(readQueue.element());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            switch (characteristic.getUuid().toString()) {
                case SampleGattAttributes.SAMPLE_RATE:
                    broadcastUpdate(DATA_AVAILABLE_SAMPLE_RATE, characteristic);
                    break;
                case SampleGattAttributes.AC_FREQ:
                    broadcastUpdate(DATA_AVAILABLE_FREQUENCY_PARAMS, characteristic);
                    break;
                case SampleGattAttributes.BIOIMPEDANCE_DATA:
                    broadcastUpdate(DATA_AVAILABLE_BIOIMPEDANCE, characteristic);
                    break;
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initialize(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        initialize(intent);
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        isBound = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.

        isBound = false;

        if (isConnected()) {
            final Intent resumeIntent = new Intent(this, OverviewActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent activityPendingIntent = PendingIntent.getActivity(
                    this, 0, resumeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.connected_to_device))
                    .setContentText(getText(R.string.connected))
                    .setContentIntent(activityPendingIntent);

            resumeIntent.putExtra(DEVICE_NAME, deviceName);
            resumeIntent.putExtra(DEVICE_ADDRESS, deviceAddress);
            startForeground(BluetoothLeService.ONGOING_NOTIFICATION_ID, notificationBuilder.build());
        }
        else {
            stopForeground(false);
            close();
        }

        return super.onUnbind(intent);
    }

    public void initialize(Intent intent) {
        String deviceName = intent.getStringExtra(DEVICE_NAME);
        String deviceAddress = intent.getStringExtra(DEVICE_ADDRESS);

        if (isInitialized || deviceAddress == null) return;

        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) showToast(this, "Unable to initialize BluetoothManager.");
        else bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) showToast(this, "Unable to obtain a BluetoothAdapter.");

        if (bluetoothManager != null && bluetoothAdapter != null) connect(deviceAddress);

        isInitialized = true;

        Log.i(TAG, "Initialized BLE connection");
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    /**
     * Used to send broadcasts that don't have attached data
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Used to send broadcasts that have attached data
     */
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] rawData = characteristic.getValue();

        if (rawData != null && rawData.length > 0) {

            switch (characteristic.getUuid().toString()) {

                case SampleGattAttributes.SAMPLE_RATE:
                    deviceStatus.sampleRate = rawData[0];
                    Log.i(TAG, "Got sample rate: " + deviceStatus.sampleRate);
                    break;

                case SampleGattAttributes.AC_FREQ:
                    deviceStatus.startFreq = rawData[0];
                    deviceStatus.stepSize = rawData[1];
                    deviceStatus.numOfIncrements = rawData[2];
                    deviceStatus.updateFrequencyVariables();

                    Log.i(TAG, "Got frequency sweep params: " + deviceStatus.startFreq + ", " + deviceStatus.stepSize + ", " + deviceStatus.numOfIncrements);
                    break;

                case SampleGattAttributes.BIOIMPEDANCE_DATA:
                    DeviceData deviceData = new DeviceData(rawData);

                    updateTaubinData(deviceData);
                    intent.putExtra(DATA_AVAILABLE_BIOIMPEDANCE, deviceData);
                    break;

                default:
                    final StringBuilder stringBuilder = new StringBuilder(rawData.length);
                    for (byte byteChar : rawData) {
                        stringBuilder.append(byteChar);
                        stringBuilder.append(" ");
                    }
                    intent.putExtra(EXTRA_DATA, new String(rawData) + "\n" + stringBuilder.toString());
                    break;
            }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            showToast(this, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (deviceAddress != null && address.equals(deviceAddress) && bluetoothGatt != null) {

            showToast(this, "Trying to use an existing mBluetoothGatt for connection.");

            if (bluetoothGatt.connect()) {
                connectionState = GATT_CONNECTING;

                broadcastUpdate(connectionState);
                showToast(this, "State changed to connecting...");
                return true;
            }
            else {
                showToast(this, "Failed connection attempt.");
                return false;
            }
        }

        final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? bluetoothDevice.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                : bluetoothDevice.connectGatt(this, false, gattCallback);

        deviceAddress = address;
        connectionState = GATT_CONNECTING;

        broadcastUpdate(connectionState);
        showToast(this, "Trying to create a new connection...");

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }

        //put the characteristic into the read queue
        readQueue.add(characteristic);

        //if there is only 1 item in the queue, then read it.  If more than 1, we handle asynchronously in the callback above
        //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
        if (readQueue.size() > 0) {
            bluetoothGatt.readCharacteristic(characteristic);
        }
    }

    public void writeCharacteristic(String uuid, int value) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);

        characteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void writeCharacteristicArray(String uuid, byte[] values) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);

        characteristic.setValue(values);
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param uuid UUID of the Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(String uuid, boolean enabled) {

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);

        if (characteristic == null) {
            showToast(this);
            return;
        }


        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

        descriptor.setValue(enabled
                ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

        bluetoothGatt.writeDescriptor(descriptor);
    }

    // Used when writing to chracteristics when receiving a callback

    public void setCharacteristicIndication(String uuid, boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            showToast(this, "BluetoothAdapter not initialized");
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);

        if (characteristic == null) {
            showToast(this);
            return;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));

        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }

    private BluetoothGattCharacteristic getCharacteristic(String uuid) {

        switch (uuid) {
            case SampleGattAttributes.SAMPLE_RATE:
                return sampleRateCharacteristic;
            case SampleGattAttributes.AC_FREQ:
                return ACFrequencyCharacteristic;
            case SampleGattAttributes.BIOIMPEDANCE_DATA:
                return impedanceCharacteristic;
            default:
                throw new IllegalArgumentException("Invalid UUID sting");
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) {
            return null;
        }

        return bluetoothGatt.getServices();
    }


    private void findGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) {
            return;
        }

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics and find the ones I'm interested in
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                switch (gattCharacteristic.getUuid().toString()) {
                    case SampleGattAttributes.BIOIMPEDANCE_DATA:
                        impedanceCharacteristic = gattCharacteristic;
                        break;
                    case SampleGattAttributes.SAMPLE_RATE:
                        sampleRateCharacteristic = gattCharacteristic;
                        readCharacteristic(sampleRateCharacteristic);
                        break;
                    case SampleGattAttributes.AC_FREQ:
                        ACFrequencyCharacteristic = gattCharacteristic;
                        readCharacteristic(ACFrequencyCharacteristic);
                        break;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(TaubinSolution taubinSolution) {
        final Intent intent = new Intent(DATA_AVAILABLE_TAUBIN_SOLUTION);
        intent.putExtra(DATA_AVAILABLE_TAUBIN_SOLUTION, taubinSolution);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateTaubinData(DeviceData deviceData) {
        curvePoints.add(deviceData.getRXpair());

        if (deviceData.freqKHZ == deviceStatus.endFreq) {
            solveTaubinAsync(curvePoints);
            curvePoints = new ArrayList<>(100);
        }

    }

    private void solveTaubinAsync(ArrayList<RXpair> circle) {
        TaubinSolution.taubinFit(circle, deviceStatus.endFreq)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(18, TimeUnit.SECONDS)
                .subscribe(this);
    }

    public boolean isConnected() {
        return connectionState.equals(GATT_CONNECTED);
    }

    public static void showToast(Context context) {
        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, resourceId, Toast.LENGTH_SHORT).show();
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

}


	/*public void writeGattDescriptor(BluetoothGattDescriptor descriptor){
        //put the descriptor into the write queue
		descriptorWriteQueue.add(descriptor);
		//if there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
		if(descriptorWriteQueue.size() > 0){
			mBluetoothGatt.writeDescriptor(descriptor);
			Log.i(TAG, "Wrote descriptor please");
		}
	}*/


		/*@Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.i(TAG, "Callback: Wrote GATT Descriptor successfully.");
				if(descriptorWriteQueue.size() > 0) {
					descriptorWriteQueue.remove();  //pop the item that we just finishing writing
				}
			}
			else{
				Log.i(TAG, "Callback: Error writing GATT Descriptor: "+ status);
			}
			Log.i(TAG, "" + descriptorWriteQueue.size());
			//if there is more to write, do it!
			if(descriptorWriteQueue.size() > 0) {
				mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
			}
		};*/
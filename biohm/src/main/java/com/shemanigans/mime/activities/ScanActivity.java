package com.shemanigans.mime.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.shemanigans.mime.R;
import com.shemanigans.mime.adapters.ScanAdapter;
import com.shemanigans.mime.services.BluetoothLeService;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity
        implements ScanAdapter.AdapterListener,
        BluetoothAdapter.LeScanCallback {

    // Declare primitives
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;    // Stops scanning after 10 seconds.

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    // Declare listview for GATT clients found
    private RecyclerView recyclerView;
    private ArrayList<BluetoothDevice> bleDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        initializeViewComponents();

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a BT adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if BT is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViewComponents() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setTitle("Scan");
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Initializes list view adapter.
        final ScanAdapter scanAdapter = new ScanAdapter(this, bleDevices);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(scanAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan, menu);

        menu.findItem(R.id.menu_stop).setVisible(!mScanning);
        menu.findItem(R.id.menu_scan).setVisible(mScanning);

        if (!mScanning) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
        else {
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                ((ScanAdapter) recyclerView.getAdapter()).clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        if (recyclerView != null) {
            ((ScanAdapter) recyclerView.getAdapter()).clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures BT is enabled on the device.  If BT is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBluetoothDeviceClicked(final BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return;
        }
        final Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        final Intent overviewActivityIntent = new Intent(ScanActivity.this, OverviewActivity.class);

        bleServiceIntent.putExtra(BluetoothLeService.DEVICE_NAME, bluetoothDevice.getName());
        bleServiceIntent.putExtra(BluetoothLeService.DEVICE_ADDRESS, bluetoothDevice.getAddress());

        overviewActivityIntent.putExtra(BluetoothLeService.DEVICE_NAME, bluetoothDevice.getName());
        overviewActivityIntent.putExtra(BluetoothLeService.DEVICE_ADDRESS, bluetoothDevice.getAddress());

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(this);
            mScanning = false;
        }

        startService(bleServiceIntent);
        startActivity(overviewActivityIntent);
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!bleDevices.contains(device)) {
                    bleDevices.add(device);
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    // Used to scan for BLE devices
    @SuppressWarnings("deprecation")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(ScanActivity.this);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(ScanActivity.this);
        }
        else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(ScanActivity.this);
        }
        invalidateOptionsMenu();
    }
}
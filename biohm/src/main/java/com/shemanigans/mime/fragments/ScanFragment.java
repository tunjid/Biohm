package com.shemanigans.mime.fragments;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.shemanigans.mime.BLEScanner;
import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseFragment;
import com.shemanigans.mime.activities.OverviewActivity;
import com.shemanigans.mime.adapters.ScanAdapter;
import com.shemanigans.mime.services.BluetoothLeService;

import java.util.ArrayList;
import java.util.List;

public class ScanFragment extends BaseFragment
        implements
        BLEScanner.BleScanCallback,
        ScanAdapter.AdapterListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;    // Stops scanning after 10 seconds.

    private boolean isScanning;

    private RecyclerView recyclerView;

    private BLEScanner scanner;
    private List<BluetoothDevice> bleDevices = new ArrayList<>();

    public static ScanFragment newInstance() {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        recyclerView.setAdapter(new ScanAdapter(this, bleDevices));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getToolBar().setTitle(R.string.button_scan);

        Activity activity = getActivity();

        boolean hasBle = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!hasBle || bluetoothAdapter == null) {
            Toast.makeText(activity, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            activity.onBackPressed();
        }

        scanner = new BLEScanner(this, bluetoothAdapter);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.scan, menu);

        menu.findItem(R.id.menu_stop).setVisible(isScanning);
        menu.findItem(R.id.menu_scan).setVisible(!isScanning);

        if (!isScanning) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
        else {
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                bleDevices.clear();
                recyclerView.getAdapter().notifyDataSetChanged();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        bleDevices.clear();
        scanLeDevice(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensures BT is enabled on the device.  If BT is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!scanner.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //scanLeDevice(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_BT);
            }
            else {
                scanLeDevice(true);
            }
        }
        else {
            scanLeDevice(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                // If request is cancelled, the result arrays are empty.
                boolean canScan = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (canScan) scanLeDevice(true);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onBluetoothDeviceClicked(final BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return;
        }
        final Intent bleServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
        final Intent overviewActivityIntent = new Intent(getActivity(), OverviewActivity.class);

        bleServiceIntent.putExtra(BluetoothLeService.DEVICE_NAME, bluetoothDevice.getName());
        bleServiceIntent.putExtra(BluetoothLeService.DEVICE_ADDRESS, bluetoothDevice.getAddress());

        String name = bluetoothDevice.getName() == null ? "Namesfbshfvsvfshfsfbwhbfhjwvswbddsajhdu" : bluetoothDevice.getName();
        overviewActivityIntent.putExtra(BluetoothLeService.DEVICE_NAME, name);
        overviewActivityIntent.putExtra(BluetoothLeService.DEVICE_ADDRESS, bluetoothDevice.getAddress());

        if (isScanning) scanLeDevice(false);

        getActivity().startService(bleServiceIntent);
        startActivity(overviewActivityIntent);
    }

    @Override
    public void onDeviceFound(final BluetoothDevice device) {
        if (!bleDevices.contains(device)) {
            bleDevices.add(device);
            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    // Used to scan for BLE devices
    private void scanLeDevice(boolean enable) {
        isScanning = enable;

        if (enable) scanner.startScan();
        else scanner.stopScan();

        getActivity().invalidateOptionsMenu();

        // Stops  after a pre-defined scan period.
        if (enable) {
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    scanner.stopScan();
                    if (getActivity() != null) getActivity().invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
        }
    }
}
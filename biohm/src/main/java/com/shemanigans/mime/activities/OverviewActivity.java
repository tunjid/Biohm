package com.shemanigans.mime.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.shemanigans.mime.NavigationDrawerFragment;
import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseActivity;
import com.shemanigans.mime.fragments.OverviewParentFragment;
import com.shemanigans.mime.services.BluetoothLeService;

import java.util.ArrayList;

public class OverviewActivity
        extends
        BaseActivity
        implements
        NavigationDrawerFragment.NavigationDrawerCallbacks,
        ServiceConnection {

    private static final String LIVE_DATA_TAG = "LIVE_DATA_TAG";
    private static final int LIVE_DATA = 0;

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private int currentFragment = -1;

    private CharSequence mTitle;

    OverviewParentFragment overviewParentFragment;

    // Code to manage Service life-cycle for BluetoothLeService.
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_term);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Intent bleIntent = new Intent(this, BluetoothLeService.class);

        setSupportActionBar(toolbar);
        bindService(bleIntent, this, BIND_AUTO_CREATE);

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mTitle = getTitle();

        // Set up the navigation drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onStop() {
        super.onStop();
        overviewParentFragment = null;
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(gattUpdateReceiver);
        unbindService(this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        ArrayList<String> fragmentTags = new ArrayList<>();

        if (fragmentManager.getFragments() != null) {
            for (Fragment f : fragmentManager.getFragments()) {
                if (f != null && f.getTag() != null) {
                    fragmentTags.add(f.getTag());
                }
            }
        }

        switch (position) {
            case LIVE_DATA:
                if (currentFragment != LIVE_DATA) {
                    fragmentTransaction
                            .replace(R.id.container,
                                    fragmentTags.contains(LIVE_DATA_TAG)
                                            ? (OverviewParentFragment) fragmentManager.findFragmentByTag(LIVE_DATA_TAG)
                                            : getNewOverViewFragment(),
                                    LIVE_DATA_TAG);

                    currentFragment = LIVE_DATA;
                    //  restoreActionBar();
                    fragmentTransaction.commit();
                }
                break;
        }

    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (overviewParentFragment == null) {
                overviewParentFragment = (OverviewParentFragment)
                        getSupportFragmentManager().findFragmentByTag(LIVE_DATA_TAG);
            }

            overviewParentFragment.receiveBroadcast(intent);
        }
    };

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private OverviewParentFragment getNewOverViewFragment() {

        final Intent intent = getIntent();

        String deviceName = intent.getStringExtra(BluetoothLeService.DEVICE_NAME);
        String deviceAddress = intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS);

        deviceName = deviceName.substring(0, deviceName.length() - 9);

        return OverviewParentFragment.newInstance(LIVE_DATA, deviceName, deviceAddress);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_CONNECTING);
        intentFilter.addAction(BluetoothLeService.GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.DATA_AVAILABLE_UNKNOWN);
        intentFilter.addAction(BluetoothLeService.DATA_AVAILABLE_SAMPLE_RATE);
        intentFilter.addAction(BluetoothLeService.DATA_AVAILABLE_BIOIMPEDANCE);
        intentFilter.addAction(BluetoothLeService.DATA_AVAILABLE_TAUBIN_SOLUTION);
        intentFilter.addAction(BluetoothLeService.DATA_AVAILABLE_FREQUENCY_PARAMS);
        return intentFilter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.menu_activity_overview, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                bluetoothLeService.connect(BluetoothLeService.deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bluetoothLeService.stopForeground(true);
                bluetoothLeService.disconnect();
                return true;
            case R.id.name_text_file:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
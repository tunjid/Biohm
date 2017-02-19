package com.shemanigans.mime.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.shemanigans.mime.NavigationDrawerFragment;
import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseActivity;
import com.shemanigans.mime.fragments.OverviewParentFragment;
import com.shemanigans.mime.services.BluetoothLeService;

public class OverviewActivity extends BaseActivity
        implements
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final int LIVE_DATA = 0;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_term);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);


        // Set up the navigation drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case LIVE_DATA:
                showFragment(getNewOverViewFragment());
                break;
        }
    }

    private OverviewParentFragment getNewOverViewFragment() {

        Intent intent = getIntent();

        String deviceName = intent.getStringExtra(BluetoothLeService.DEVICE_NAME);
        String deviceAddress = intent.getStringExtra(BluetoothLeService.DEVICE_ADDRESS);

        return OverviewParentFragment.newInstance(deviceName, deviceAddress);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            getMenuInflater().inflate(R.menu.menu_activity_overview, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.name_text_file:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
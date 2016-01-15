package com.shemanigans.mime.abstractclasses;

import android.support.v7.app.AppCompatActivity;

import com.shemanigans.mime.services.BluetoothLeService;

/**
 * Base Activity class
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected BluetoothLeService bluetoothLeService;

    public BluetoothLeService getBleService() {
        return bluetoothLeService;
    }
}

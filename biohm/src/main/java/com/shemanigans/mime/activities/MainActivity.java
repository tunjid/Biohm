package com.shemanigans.mime.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.shemanigans.mime.R;
import com.shemanigans.mime.abstractclasses.BaseActivity;
import com.shemanigans.mime.fragments.ScanFragment;
import com.shemanigans.mime.fragments.StartFragment;


public class MainActivity extends BaseActivity {

    public static final String GO_TO_SCAN = "GO_TO_SCAN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            boolean goToScan = getIntent().getBooleanExtra(GO_TO_SCAN, false);
            showFragment(goToScan ? ScanFragment.newInstance() : StartFragment.newInstance());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}

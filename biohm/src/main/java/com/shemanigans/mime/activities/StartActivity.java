package com.shemanigans.mime.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.shemanigans.mime.ButtonNo;
import com.shemanigans.mime.R;


public class StartActivity extends ActionBarActivity {
	public final static String EXTRA_MESSAGE = "com.shemanigans.mime.MESSAGE";
    private Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	// Called when the user clicks the "Yes" button
	
	public void buttonYes(View view) {
		Intent intent = new Intent(this, PairActivity.class);
		startActivity(intent);
	}
	
	// Called when the user clicks the "No" button

	public void buttonNo(View view) {
		Intent intent = new Intent(this, ButtonNo.class);
		startActivity(intent);
	}

}

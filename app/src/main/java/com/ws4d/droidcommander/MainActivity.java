package com.ws4d.droidcommander;

import java.io.IOException;

import org.ws4d.java.io.fs.FileSystem;
import org.ws4d.java.platform.io.fs.LocalFileSystem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String TAG = "DPWSExample";
	Intent intentExplore = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			LocalFileSystem lfs = ((LocalFileSystem)FileSystem.getInstance());
			lfs.setAndroidContext(getApplicationContext());
		}
	
		catch (IOException e) {e.printStackTrace();}
		// Multicast aktivieren
		allowMulticast();
		
		setContentView(R.layout.activity_main);
		Intent intent;
		Toast.makeText(this,"Service Binding in Main Activity",3).show();
		intent = new Intent(this, DPWSService.class);

		startService(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void switchToExploreView(View view)
	{
		try{
			Toast.makeText(this,"Entered 1 in Main, Intent created earlier",3).show();

			intentExplore = new Intent(this, ExploreActivity.class);

			Toast.makeText(this,"Entered 2 in main intent. Initializing.",3).show();
			startActivity(intentExplore);
		}
		catch (Exception e){
			Log.i(TAG, ""+e.getMessage());
			Toast.makeText(this,e.getMessage(),5).show();
		}

		}

	
	public void switchToSettingsView(View view)
	{
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	private void allowMulticast() {
    	WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null) {
			// Allows an application to receive Wifi Multicast packets. Normally
			// the Wifi stack filters out packets not explicitly addressed to
			// this device.
			MulticastLock mcLock = wifiManager
					.createMulticastLock("DPWS_Multicast_lock");
			mcLock.acquire();
			Log.d(TAG, "     [ExploreActivity] MULTICAST-LOCK ACQUIRED!");

			// Getting the WIFI-IPADDRESS from the System
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			Log.d(TAG, "<ANDROID> WIFI-INFO:" + wifiInfo.toString());
		}
    	
    }
}
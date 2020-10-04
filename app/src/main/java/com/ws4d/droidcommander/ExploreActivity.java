package com.ws4d.droidcommander;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ExploreActivity extends Activity implements OnItemClickListener {
	static final int MSG_DEVICE_FOUND = 1;

	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mBound = false;

	@SuppressLint("HandlerLeak")
	class myHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DPWSService.MSG_SET_VALUE:
				TextView debugText = (TextView) findViewById(R.id.debugTextField);
				debugText.setText("Received from service: " + msg.arg1);
				break;
			case DPWSService.MSG_SEARCH_DEVICES:
				// Toast.makeText(ExploreActivity.this, "EA:Device Found.",
				// Toast.LENGTH_LONG).show();
				break;
			case MSG_DEVICE_FOUND:
				if (msg.obj.toString()
						.contains("schemas.microsoft.com/windows"))
					break;
				adapter.add(msg.obj.toString());
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target published for clients to send messages to IncomingHandler.
	 */
	final Messenger myMessenger = new Messenger(new myHandler());

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			mService = new Messenger(service);
//			TextView debugText = (TextView) findViewById(R.id.debugTextField);
//			debugText.setText("Attached.");

			mBound = true;
			Log.d(Constants.TAG, "     [ExploreActivity] Service Bound!");

			try {
				Message msg = Message.obtain(null,
						DPWSService.MSG_REGISTER_CLIENT);
				msg.arg1 = DPWSService.EXPLORE_ACTIVITY;
				msg.replyTo = myMessenger;
				mService.send(msg);

				// the DPWSService already knows which device we are dealing
				// with here
				msg = Message.obtain(null, DPWSService.MSG_SEARCH_DEVICES);
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d(Constants.TAG, "     [ExploreActivity] Service Disconnected!");
			mService = null;
			TextView debugText = (TextView) findViewById(R.id.debugTextField);
			debugText.setText("Disconnected.");
		}
	};

	ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_explore);

		// Show the Up button in the action bar.
		setupActionBar();


		// connect to DPWSService
		Intent intent = new Intent(this, DPWSService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		// create ListView for found devices and set up Adapter that handlees
		// the List
		final ListView listView = (ListView) findViewById(R.id.exploreListView1);
		ArrayList<String> list = new ArrayList<String>();

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_expandable_list_item_1, list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);

	}

	protected void onDestroy() {
		super.onDestroy();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Explore");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.explore, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
		final String item = (String) parent.getItemAtPosition(index);

		// TextView debugText = (TextView) findViewById(R.id.debugTextField);
		// debugText.setText(item);

		// TODO intent mit Informationen fülen
		Intent intent = new Intent(this, DeviceActivity.class);
		intent.putExtra(Constants.EXTRA_DEVICE_NAME, item);
		intent.putExtra(Constants.EXTRA_DEVICE_INDEX, index);
		try {
			Message msg = Message
					.obtain(null, DPWSService.MSG_SET_DEVICE_INDEX);
			Log.d(Constants.TAG,
					"[ExploreActiity] Device Index of clicked device is: "
							+ index);
			msg.arg1 = index;
			mService.send(msg);
		} catch (RemoteException e) {
		}

		startActivity(intent);
	}

	public void refreshDevices(View view) {
		clearListOfDevices(view);
		// Toast.makeText(this, "EA:Search request sent.", Toast.LENGTH_SHORT)
		// .show();
		try {
			Message msg = Message.obtain(null, DPWSService.MSG_SEARCH_DEVICES);
			// msg.replyTo = myMessenger;
			mService.send(msg);
		} catch (RemoteException e) {
		}

		clearListOfDevices(view);
	}

	public void clearListOfDevices(View view) {
		adapter.clear();
	}
}
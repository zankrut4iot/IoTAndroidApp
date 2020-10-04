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

public class DeviceActivity extends Activity implements OnItemClickListener {
	/** Messages that can be received */
	static final int MSG_HAVE_DEVICE_INFO = 0;
	static final int MSG_EMPTY_MESSAGE_2 = 1;

	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mBound = false;

	int deviceIndex;

	@SuppressLint("HandlerLeak")
	class myHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_HAVE_DEVICE_INFO:
				// Toast.makeText(DeviceActivity.this,
				// "DA:Service has Metadata.",
				// Toast.LENGTH_SHORT).show();
				fillInDeviceInfo(msg.getData());
				break;
			case MSG_EMPTY_MESSAGE_2:
				// Toast.makeText(DeviceActivity.this, "Empty Message 2",
				// Toast.LENGTH_SHORT).show();
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
			mService = new Messenger(service);
			// Toast.makeText(DeviceActivity.this, "OA:Service connected.",
			// Toast.LENGTH_LONG).show();

			mBound = true;
			Log.d(Constants.TAG, "     [DeviceActivity] Service Connected!");

			try {
				Message msg = Message.obtain(null,
						DPWSService.MSG_REGISTER_CLIENT);
				msg.arg1 = DPWSService.DEVICE_ACTIVITY;
				msg.replyTo = myMessenger;
				mService.send(msg);

				msg = Message.obtain(null, DPWSService.MSG_GET_METADATA);
				msg.arg1 = deviceIndex;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d(Constants.TAG, "     [DeviceActivity] Service Disconnected!");
			mService = null;
			// Toast.makeText(DeviceActivity.this, "DA:Service disconnected.",
			// Toast.LENGTH_LONG).show();

		}
	};

	protected void onDestroy() {
		super.onDestroy();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	protected ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);

		// mit DPWSService verbinden
		Intent intent = new Intent(this, DPWSService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		// Interface aufbauen
		// Intent activityIntent = getIntent();
		// String deviceName = activityIntent
		// .getStringExtra(Constants.EXTRA_DEVICE_NAME);
		// TextView deviceNameTextView = (TextView)
		// findViewById(R.id.deviceNameTextView);
		// Show the Up button in the action bar.
		setupActionBar("Device");
		// deviceNameTextView.setText(deviceName);

		// deviceIndex für alle funktionen des Activities verfügbar machen, vor
		// allem zum Metadaten holen
		// deviceIndex =
		// activityIntent.getIntExtra(Constants.EXTRA_DEVICE_INDEX, 0);

		final ListView listView = (ListView) findViewById(R.id.deviceListView);
		ArrayList<String> list = new ArrayList<String>();

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_expandable_list_item_1, list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);

		// dummy um liste zu füllen
		// adapter.add("Operation Nummero 1");
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar(String title) {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Device");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.device, menu);
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

	private void fillInDeviceInfo(Bundle data) {
		TextView deviceName = (TextView) findViewById(R.id.deviceNameTextView);
		deviceName
				.setText(data.getCharSequence(Constants.DEVICE_FRIENDLY_NAME));
		String deviceInfoText = "Firmwareversion: ";
		deviceInfoText = deviceInfoText
				+ data.getCharSequence(Constants.DEVICE_FIRMWARE_VERSION);
		deviceInfoText = deviceInfoText + "\nManufacturer: ";
		deviceInfoText = deviceInfoText
				+ data.getCharSequence(Constants.DEVICE_MANUFACTURER);
		deviceInfoText = deviceInfoText + "\n";
		TextView deviceInfo = (TextView) findViewById(R.id.metaDataTextView);
		deviceInfo.setText(deviceInfoText);

		ArrayList<String> portList = data
				.getStringArrayList(Constants.DEVICE_SERVICE_LIST);

		if (portList.size() == 0) {
			return;
		}

		for (int i = 0; i < portList.size(); i++) {
			adapter.add(portList.get(i));
		}

		deviceIndex = data.getInt(Constants.EXTRA_DEVICE_INDEX);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
		// start new activity which represents device
		final String item = (String) parent.getItemAtPosition(index);

		// TextView debugText = (TextView) findViewById(R.id.debugTextField);
		// debugText.setText(item);

		// fill intent with necessary information
		Intent intent = new Intent(this, ServiceActivity.class);
		intent.putExtra(Constants.EXTRA_SERVICE_NAME, item);
		intent.putExtra(Constants.EXTRA_SERVICE_INDEX, index);
		startActivity(intent);
	}

}

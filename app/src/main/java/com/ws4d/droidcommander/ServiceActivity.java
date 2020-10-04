package com.ws4d.droidcommander;

import java.util.ArrayList;
import java.util.Iterator;

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

public class ServiceActivity extends Activity implements OnItemClickListener {
	/** Messages that can be received */
	static final int MSG_HAVE_WSDL = 0;
	static final int MSG_OPERATION_DETAILS = 1;

	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mBound = false;

	@SuppressLint("HandlerLeak")
	class myHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_HAVE_WSDL:
				updateOperationsList(msg.getData());
				// Toast.makeText(ServiceActivity.this, "Service has WSDL",
				// Toast.LENGTH_SHORT).show();
				break;
			case MSG_OPERATION_DETAILS:
				// Toast.makeText(ServiceActivity.this,
				// "Incoming Operation Details", Toast.LENGTH_SHORT)
				// .show();
				startOperationActivity(msg.getData());
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
			// Toast.makeText(ServiceActivity.this, "SA:Service connected.",
			// Toast.LENGTH_LONG).show();

			mBound = true;
			Log.d(Constants.TAG, "     [ServiceActivity] Service Connected!");

			try {
				Message msg = Message.obtain(null,
						DPWSService.MSG_REGISTER_CLIENT);
				msg.arg1 = DPWSService.SERVICE_ACTIVITY;
				msg.replyTo = myMessenger;
				mService.send(msg);

				msg = Message.obtain(null, DPWSService.MSG_GET_WSDL);
				msg.arg1 = getIntent().getExtras().getInt(Constants.EXTRA_SERVICE_INDEX);
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d(Constants.TAG, "     [ServiceActivity] Service Disconnected!");
			mService = null;
			// Toast.makeText(ServiceActivity.this, "SA:Service disconnected.",
			// Toast.LENGTH_LONG).show();

		}
	};

	protected ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

			super.onCreate(savedInstanceState);

			setContentView(R.layout.activity_service);
			// Show the Up button in the action bar.
			setupActionBar();

			// mit DPWSService verbinden
			Intent intent = new Intent(this, DPWSService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

			final ListView listView = (ListView) findViewById(R.id.serviceListView);
			ArrayList<String> list = new ArrayList<String>();

			adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_expandable_list_item_1, list);
			listView.setAdapter(adapter);
			listView.setOnItemClickListener(this);

	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Service");
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

	private void updateOperationsList(Bundle data) {
		TextView serviceInfo = (TextView) findViewById(R.id.serviceInfoTextView);
		serviceInfo.setText((CharSequence) data
				.get(Constants.EXTRA_SERVICE_NAME));
		ArrayList<String> operations = data
				.getStringArrayList(Constants.DEVICE_SERVICE_OPERATIONS_LIST);
		Iterator<String> operationsIterator = operations.iterator();
		while (operationsIterator.hasNext()) {
			adapter.add(operationsIterator.next());
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
		try {
			Message msg = Message.obtain(null,
					DPWSService.MSG_GET_OPERATION_DETAILS);
			Bundle data = new Bundle();
			data.putInt(Constants.EXTRA_OPERATION_INDEX, index);
			msg.setData(data);
			mService.send(msg);
		} catch (RemoteException e) {
		}
	}

	private void startOperationActivity(Bundle data) {
		// TODO intent mit Informationen füllen
		Intent intent = new Intent(this, OperationActivity.class);
		// intent.putExtra(Constants.EXTRA_OPERATION_NAME,
		// data.getString(Constants.EXTRA_OPERATION_NAME));
		// intent.putExtra(Constants.EXTRA_NUMBER_OF_INPUT_VALUES,
		// data.getInt(Constants.EXTRA_NUMBER_OF_INPUT_VALUES));
		// intent.putExtra(Constants.EXTRA_INPUT_VALUE_NAMES, value);
		// intent.putExtra(Constants.EXTRA_INPUT_VALUE_TYPES, value);
		intent.putExtras(data);
		startActivity(intent);
	}
}

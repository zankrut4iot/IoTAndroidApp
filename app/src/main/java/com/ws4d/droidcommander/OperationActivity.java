package com.ws4d.droidcommander;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class OperationActivity extends Activity {
	/** Messages that can be received */
	static final int MSG_DISPLAY_RESULT = 0;
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
			case MSG_DISPLAY_RESULT:
				displayResult(msg.getData());
				// Toast.makeText(OperationActivity.this,
				// msg.getData().getString(Constants.EXTRA_RESULT),
				// Toast.LENGTH_LONG).show();
				break;
			case MSG_EMPTY_MESSAGE_2:
				// Toast.makeText(OperationActivity.this, "Empty Message 2",
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
			// Toast.makeText(OperationActivity.this, "OA:Service connected.",
			// Toast.LENGTH_LONG).show();

			mBound = true;
			Log.d(Constants.TAG, "     [OperationActivity] Service Connected!");

			try {
				Message msg = Message.obtain(null,
						DPWSService.MSG_REGISTER_CLIENT);
				msg.arg1 = DPWSService.OPERATION_ACTIVITY;
				msg.replyTo = myMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.d(Constants.TAG,
					"     [OperationActivity] Service Disconnected!");
			mService = null;
			// Toast.makeText(OperationActivity.this,
			// "OA:Service disconnected.",
			// Toast.LENGTH_LONG).show();

		}
	};

	/**
	 * The Bundle of data that contains all the important information about the
	 * operations.
	 * 
	 * @Contains Constants.EXTRA_OPERATION_NAME
	 *           Constants.EXTRA_INPUT_VALUE_NAMES
	 *           Constants.EXTRA_INPUT_VALUE_TYPES
	 *           Constants.EXTRA_NUMBER_OF_INPUT_VALUES
	 *           Constants.EXTRA_OPERATION_INDEX
	 */
	private Bundle data = new Bundle();

	/**
	 * Contains all possiblevalueFields.
	 */
	ArrayList<EditText> editText = new ArrayList<EditText>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_operation);
		// Show the Up button in the action bar.
		setupActionBar();

		// mit DPWSService verbinden
		Intent intent = new Intent(this, DPWSService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		// Interface aufbauen
		Intent activityIntent = getIntent();
		data = activityIntent.getExtras();

		String operationName = data.getString(Constants.EXTRA_OPERATION_NAME);

		TextView operationNameTextView = (TextView) findViewById(R.id.operationNameTextView);
		operationNameTextView.setText(operationName);

		int neededInputValues = data.getInt(
				Constants.EXTRA_NUMBER_OF_INPUT_VALUES, 0);
		// int neededOutputValues = 1;

		ArrayList<String> inputValueName = data
				.getStringArrayList(Constants.EXTRA_INPUT_VALUE_NAMES);
		ArrayList<String> inputValueType = data
				.getStringArrayList(Constants.EXTRA_INPUT_VALUE_TYPES);

		// for each input Parameter a new editText is added to the Linear Layout
		// in Activity_operation.xml
		LinearLayout linearL = (LinearLayout) findViewById(R.id.InnerOpertionLayout);
		for (int i = 0; i < neededInputValues; i++) {
			// editText.add(i,new EditText(this));
			// editText.get(i).setHint(inputValueName.get(i) + " " +
			// inputValueType.get(i));
			EditText editT = new EditText(this);
			editT.setHint(inputValueName.get(i) + " " + inputValueType.get(i));
			
			LayoutParams layoutP = new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
			linearL.addView(editT, layoutP);
			editText.add(i, editT);
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		Log.d(Constants.TAG, "onPause");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		Log.d(Constants.TAG, "onStop");
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(Constants.TAG, "onDestroy");
	}
	

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle("Operation");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.operation, menu);
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

	public void invoke(View view) {
		// TODO: invoke auf Knopfdruck implementieren
		// collect the values
		// clear what has been typed in
		// send Message to DPWSService
		try {
			// Bundle data = new Bundle();
			// TODO: data mit Daten füllen
			Message msg = Message
					.obtain(null, DPWSService.MSG_INVOKE_OPERATION);
			/*ArrayList<String> values = new ArrayList<String>();
			for (int i = 0; i < data
					.getInt(Constants.EXTRA_NUMBER_OF_INPUT_VALUES); i++) {
				values.add(i, editText.get(i).getText().toString());
				Log.d(Constants.TAG, "getText().toString()"
						+ editText.get(i).getText().toString());
			}
			data.putStringArrayList(Constants.EXTRA_INPUT_VALUES, values);
			msg.setData(data);
			mService.send(msg);*/
		} catch (Exception e) {
		}

	}

	private Activity getActivity() {
		return this;
	}

	private void displayResult(Bundle data) {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(
				this.getActivity());

		// 2. Chain together various setter methods to set the dialog
		// characteristics
		builder.setMessage(data.getString(Constants.EXTRA_RESULT)).setTitle(
				"Result of your Operation");
		builder.setPositiveButton("Discard", null);

		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
		dialog.show();
	}

}

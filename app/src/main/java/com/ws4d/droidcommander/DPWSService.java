package com.ws4d.droidcommander;

import java.util.ArrayList;
import java.util.HashSet;

import org.ws4d.java.JMEDSFramework;
import org.ws4d.java.authorization.AuthorizationException;
import org.ws4d.java.authorization.AuthorizationManager;
import org.ws4d.java.client.DefaultClient;
import org.ws4d.java.client.HelloListener;
import org.ws4d.java.client.SearchCallback;
import org.ws4d.java.client.SearchManager;
import org.ws4d.java.communication.CommunicationException;
import org.ws4d.java.communication.CommunicationManager;
import org.ws4d.java.communication.CommunicationManagerRegistry;
import org.ws4d.java.communication.DPWSCommunicationManager;
import org.ws4d.java.communication.DPWSProtocolInfo;
import org.ws4d.java.communication.DPWSProtocolVersion;
import org.ws4d.java.communication.structures.OutgoingDiscoveryInfo;
import org.ws4d.java.configuration.DPWSProperties;
import org.ws4d.java.configuration.DeviceProperties;
import org.ws4d.java.dispatch.DefaultServiceReference;
import org.ws4d.java.eventing.ClientSubscription;
import org.ws4d.java.eventing.EventListener;
import org.ws4d.java.eventing.EventSink;
import org.ws4d.java.schema.ComplexType;
import org.ws4d.java.schema.Element;
import org.ws4d.java.security.CredentialInfo;
import org.ws4d.java.security.SecurityKey;
import org.ws4d.java.service.Device;
import org.ws4d.java.service.InvocationException;
import org.ws4d.java.service.Operation;
import org.ws4d.java.service.Service;
import org.ws4d.java.service.listener.ServiceListener;
import org.ws4d.java.service.parameter.ParameterValue;
import org.ws4d.java.service.parameter.ParameterValueManagement;
import org.ws4d.java.service.reference.DeviceReference;
import org.ws4d.java.service.reference.ServiceReference;
import org.ws4d.java.structures.DataStructure;
import org.ws4d.java.structures.HashMap;
import org.ws4d.java.structures.Iterator;
import org.ws4d.java.structures.ListIterator;
import org.ws4d.java.structures.Set;
import org.ws4d.java.types.HelloData;
import org.ws4d.java.types.LocalizedString;
import org.ws4d.java.types.QName;
import org.ws4d.java.types.QNameSet;
import org.ws4d.java.types.SearchParameter;
import org.ws4d.java.types.URI;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class DPWSService extends android.app.Service implements SearchCallback {
	// Command to the service to register a client, receiving callbacks from the
	// service. The Message's replyTo field must be a Messenger of the client
	// where callbacks should be sent.
	static final int MSG_REGISTER_CLIENT = 1;
	// Command to the service to unregister a client, ot stop receiving
	// callbacks from the service. The Message's replyTo field must be a
	// Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	static final int MSG_UNREGISTER_CLIENT = 2;
	// Command to service to set a new value. This can be sent to the service to
	// supply a new value, and will be sent by the service to any registered
	// clients with the new value.
	static final int MSG_SET_VALUE = 3;
	// Command to the service to initiate a search for devices.
	static final int MSG_SEARCH_DEVICES = 4;
	// Command to set the device index of the device that is dealt with at the
	// moment
	static final int MSG_SET_DEVICE_INDEX = 5;
	// Command to the service to retrieve device metadata
	static final int MSG_GET_METADATA = 6;
	// Command to the service to get a services WSDL. Arg1 must beServiceIndex.
	static final int MSG_GET_WSDL = 7;
	// Command to the Service to return number of input values, their types and
	// names.
	static final int MSG_GET_OPERATION_DETAILS = 8;
	// Command to the service to invoke the specified operation on a certain
	// device.
	static final int MSG_INVOKE_OPERATION = 9;

	static final int EXPLORE_ACTIVITY = 0;
	static final int DEVICE_ACTIVITY = 1;
	static final int SERVICE_ACTIVITY = 2;
	static final int OPERATION_ACTIVITY = 3;

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	int deviceIndex = -1;

	@SuppressLint("HandlerLeak")
	class myHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_REGISTER_CLIENT:
					// register this client as a new client if no client
					// Client of this kind has been registered yet
					try {
						mClients.set(msg.arg1, msg.replyTo);
					} catch (IndexOutOfBoundsException e) {
						if (mClients.size() == msg.arg1)
							mClients.add(msg.arg1, msg.replyTo);
					}
					break;
				case MSG_UNREGISTER_CLIENT:
					mClients.remove(msg.arg1);
					break;
				case MSG_SEARCH_DEVICES: // send by ExploreActivity
					searchDevices();
					break;
				case MSG_SET_DEVICE_INDEX: // send by ExploreActivity
					deviceIndex = msg.arg1;
					break;
				case MSG_GET_METADATA: // send by DeviceActivity
					getMetadata();
					break;
				case MSG_GET_WSDL:
					getWSDL(msg.arg1);
					break;
				case MSG_GET_OPERATION_DETAILS: // send by ServiceActivity
					getOperationDetails(msg.getData().getInt(
							Constants.EXTRA_OPERATION_INDEX));
					break;
				case MSG_INVOKE_OPERATION: // send by OperationActivity
					invokeOperation(msg.getData());
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	final Messenger myMessenger = new Messenger(new myHandler());

	Boolean DPWS_Running = false;
	private Thread t;
	private ArrayList<DeviceReference> deviceReferences = new ArrayList<DeviceReference>();
	private Device device;
	private Service service;
	private ArrayList<Operation> operationList = new ArrayList<Operation>();

	// Binder der den Clients Ÿbergeben wird
	public IBinder onBind(Intent intent) {
		// Toast.makeText(this, "Service:local_service_bound",
		// Toast.LENGTH_SHORT)
		// .show();
		return myMessenger.getBinder();
	}

	public DPWSService() {
		super();
		t = new Thread() {

			public void run() {
				DPWSProperties properties = DPWSProperties.getInstance();
				properties.addSupportedDPWSVersion(DPWSProtocolVersion.DPWS_VERSION_2009);
				org.ws4d.java.util.Log.setLogLevel(org.ws4d.java.util.Log.DEBUG_LEVEL_INFO);

				JMEDSFramework.start(null);
				DPWS_Running = true;
				Log.d(Constants.TAG,
						"     [DPWSService] starting DPWS Framework.");

				// create client
				HelloClient client = new HelloClient(DPWSService.this);

				// register hello listening for all possible domains
				client.registerHelloListening();
			}
		};
		t.start();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(Constants.TAG, "     [DPWSService] Service created.");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Constants.TAG, "     [DPWSService] Service destroyed.");
	}

	public class DPWSBinder extends Binder {

		DPWSService getService() {
			// liefert die Instanz von DPWSService damit Clients public methods
			// benutzen kšnnen
			return DPWSService.this;
		}
	}

	/**
	 * Initiates a search for devices. Clears list of deviceReferences first.
	 */
	public void searchDevices() {
		deviceReferences.clear();
		if (DPWS_Running) {
			try{
				DefaultClient defaultClient = new DefaultClient();
				Log.d(Constants.TAG, "     [DPWSService] Searching Devices.");
				SearchParameter sp = new SearchParameter();

				sp.setDeviceTypes (new QNameSet(new QName("test","namespace")));
				//defaultClient.searchDevice(sp);
				SearchManager.searchDevice(null, this, null); // Search for devices

			}catch (Exception e){
				Log.d(Constants.TAG, "     [DPWSService] In Exception.");
				e.printStackTrace();
			}
				}
	}


	public void searchService() {
	}

	@Override
	public void deviceFound(DeviceReference devRef, SearchParameter search) {
		String obj = new String("");
		try {
			for (Iterator i = devRef.getDevicePortTypes(false); i.hasNext();) {
				obj = ((QName) i.next()).getLocalPart();
				if (!obj.equals("Device")) {
					break;
				}
			}
		} catch (CommunicationException e1) {
		}

		try {
			mClients.get(EXPLORE_ACTIVITY)
					.send(Message.obtain(null,
							ExploreActivity.MSG_DEVICE_FOUND, obj));
		} catch (RemoteException e) {
		}

		deviceReferences.add(devRef);
		Log.d(Constants.TAG,
				"     [DPWSService] Device Found" + devRef.toString());
	}

	@Override
	public void serviceFound(ServiceReference servRef, SearchParameter search) {
	}

	/**
	 *
	 *
	 */
	public void getMetadata() {
		try {
			device = deviceReferences.get(deviceIndex).getDevice();

			Bundle data = new Bundle();
			String name = "";
			Iterator names = device.getFriendlyNames();
			if (names.hasNext())
				name = ((LocalizedString) names.next()).getValue();

			data.putCharSequence(Constants.DEVICE_FRIENDLY_NAME, name);
			data.putCharSequence(Constants.DEVICE_MANUFACTURER,
					((LocalizedString) device.getManufacturers().next())
							.getValue());
			data.putCharSequence(Constants.DEVICE_FIRMWARE_VERSION,
					device.getFirmwareVersion());

			Iterator serviceReferences = device
					.getServiceReferences(SecurityKey.EMPTY_KEY);
			ArrayList<String> serviceList = new ArrayList<String>();
			while (serviceReferences.hasNext()) {
				URI myURI = (((ServiceReference) serviceReferences.next())
						.getServiceId());
				// substring after the last '/' or the entire String
				String subString = myURI.getPath();
				int index = subString.lastIndexOf('/') + 1;
				serviceList.add(subString.substring(index));
			}
			data.putStringArrayList(Constants.DEVICE_SERVICE_LIST, serviceList);
			try {
				Message msg = Message.obtain(Message.obtain(null,
						DeviceActivity.MSG_HAVE_DEVICE_INFO));
				msg.setData(data);
				mClients.get(DEVICE_ACTIVITY).send(msg);
			} catch (RemoteException e) {
			}
		} catch (CommunicationException e) {
			// Toast.makeText(this, "Service:TimeoutException",
			// Toast.LENGTH_SHORT)
			// .show();
			Log.d(Constants.TAG,
					"[DPWSService] getMetadata threw Communicationsexception");
//			getMetadata();
		}

	}

	/**
	 * Retrieves respective Service with respective serviceIndex (at the moment
	 * always 0) and gets a list of available Operations.
	 *
	 * @param serviceIndex
	 */
	public void getWSDL(int serviceIndex) {
		// we don't expect more than one hosted Service to be present
		// so service Index is set to 0 (the first and only service)
		Log.d(Constants.TAG, "     [DPWSService] ServiceReference Index: " + serviceIndex);
//		serviceIndex = 0;
		Iterator serviceReferences = device.getServiceReferences(SecurityKey.EMPTY_KEY);
		DefaultServiceReference servRef = (DefaultServiceReference) serviceReferences.next();
		for (int i = 0; i < serviceIndex; i++)
		{
			servRef = (DefaultServiceReference) serviceReferences.next();
		}
//		DefaultServiceReference servRef = (DefaultServiceReference) serviceReferences.next();
		Log.d(Constants.TAG, "     [DPWSService] ServiceReference: " + servRef);

		Bundle data = new Bundle();
		// getServiceName
		// substring after the last '/' or the entire String
		String path = servRef.getServiceId().getPath();
		int index = path.lastIndexOf('/') + 1;
		data.putCharSequence(Constants.EXTRA_SERVICE_NAME,
				(path.substring(index)));
		try {
			service = servRef.getService();
			Iterator operations = service.getAllOperations();

			ArrayList<String> operationNameList = new ArrayList<String>();
			int operationIndex = 0;
			while (operations.hasNext()) {
				Operation operation = (Operation) operations.next();
				operationList.add(operationIndex, operation);
				operationNameList.add(operationIndex, operation.getName());
				operationIndex++;
			}
			// Bundle data = new Bundle();
			data.putStringArrayList(Constants.DEVICE_SERVICE_OPERATIONS_LIST,
					operationNameList);
			Message msg = Message.obtain(Message.obtain(null,
					ServiceActivity.MSG_HAVE_WSDL));
			msg.setData(data);
			mClients.get(SERVICE_ACTIVITY).send(msg);

		} catch (CommunicationException e) {
			Log.d(Constants.TAG,
					"     [DPWSService] Timeout Exception leider aufgetreten.");
		} catch (RemoteException e) {
		}
	}

	/**
	 *
	 * @param operationIndex
	 */
	public void getOperationDetails(int operationIndex) {
		Operation operation = operationList.get(operationIndex);
		int numberOfInputValues = 0;
		ParameterValue input = operation.createInputValue();

		ArrayList<String> inputNames = new ArrayList<String>();
		ArrayList<String> inputTypes = new ArrayList<String>();

		if (input != null) {
			if (input.getType().isComplexType()) {
				ComplexType complexType = (ComplexType) input.getType();
				Iterator elements = complexType.elements();
				while (elements.hasNext()) {
					Element element = (Element) elements.next();
					if (!element.getType().isComplexType()) {
						String name = element.getName().getLocalPart();
						Log.d(Constants.TAG,
								"     [DPWSService][Operation] element.getName().getLocalPart() => "
										+ name);
						String type = "("
								+ element.getType().getName().getLocalPart()
								+ ")";
						Log.d(Constants.TAG,
								"     [DPWSService][Operation] element.getType().getName().getLocalPart() =>  "
										+ type);
						if (element.getMinOccurs() < 1) {
							type = type.concat(" <optional>");
						}

						inputNames.add(name);
						numberOfInputValues++;
						inputTypes.add(type);
					}

				}

			} else {
				Log.d(Constants.TAG,
						"     [DPWSService][Operation] input.getName().getLocalPart() => "
								+ input.getName().getLocalPart());
				Log.d(Constants.TAG,
						"     [DPWSService][Operation] input.getType() =>  "
								+ input.getType());

				inputNames.add(input.getName().getLocalPart());
				numberOfInputValues++;
				inputTypes.add(input.getType().getName().getLocalPart());
			}
		}
		try {
			Bundle data = new Bundle();
			data.putInt(Constants.EXTRA_OPERATION_INDEX, operationIndex);
			data.putStringArrayList(Constants.EXTRA_INPUT_VALUE_NAMES,
					inputNames);
			data.putStringArrayList(Constants.EXTRA_INPUT_VALUE_TYPES,
					inputTypes);
			data.putInt(Constants.EXTRA_NUMBER_OF_INPUT_VALUES,
					numberOfInputValues);
			data.putString(Constants.EXTRA_OPERATION_NAME, operation.getName());
			Message msg = Message.obtain(null,
					ServiceActivity.MSG_OPERATION_DETAILS);
			msg.setData(data);
			mClients.get(SERVICE_ACTIVITY).send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called in order to invoke an operation.
	 *
	 * @param msg test
	 */
	public void invokeOperation(Bundle data) {
		Operation operation = operationList.get(data
				.getInt(Constants.EXTRA_OPERATION_INDEX));
		ParameterValue input = operation.createInputValue();

		ArrayList<String> inputValueName = data
				.getStringArrayList(Constants.EXTRA_INPUT_VALUE_NAMES);
		ArrayList<String> values = data
				.getStringArrayList(Constants.EXTRA_INPUT_VALUES);

		for (int i = 0; i < data.getInt(Constants.EXTRA_NUMBER_OF_INPUT_VALUES); i++) {
			ParameterValueManagement.setString(input, inputValueName.get(i),
					values.get(i));
		}
		// ParameterValueManagement.setString(input, "value","3.7");
		// ParameterValueManagement.setString(input, "unit", "C");
		ParameterValue result = null;
		try {
			result = operation.invoke(input,
					CredentialInfo.EMPTY_CREDENTIAL_INFO);
			String resultString = "";

			if (result != null)
			{
				ListIterator children = result.getChildrenList();
				// Log.d(Constants.TAG,
				// "[DPWSService][Children] child.toString() = "+
				// children.next().toString());
				// Log.d(Constants.TAG,
				// "[DPWSService][Children] child.toString() = "+
				// children.next().toString());
				// Log.d(Constants.TAG,
				// "[DPWSService][Children] child.toString() = "+
				// children.next().toString());

				if (result.getType().isComplexType()) {
					ComplexType complexType = (ComplexType) result.getType();
					Iterator elements = complexType.elements();
					while (elements.hasNext()) {
						Element element = (Element) elements.next();
						if (!element.getType().isComplexType()) {
							Log.d(Constants.TAG,
									"     [DPWSService][result] element.getName().getLocalPart() => "
											+ element.getName().getLocalPart());
							// Log.d(Constants.TAG,
							// "     [DPWSService][result] element.getType().getName().getLocalPart() =>  "
							// + element.getType().getName()
							// .getLocalPart());

							resultString = resultString.concat(element.getName()
									.getLocalPart());
							resultString = resultString.concat(": ");
							resultString = resultString.concat(children.next()
									.toString());
							resultString = resultString.concat("\n");

							Log.d(Constants.TAG,
									"     [DPWSService][result] resultString = "
											+ resultString);
						}

					}
				} else {
					resultString = result.toString();
				}

				Log.d(Constants.TAG, "[DPWSService][result] result to string "
						+ result.toString());

				// TODO Result zurŸckschicken
				Bundle resultData = new Bundle();
				resultData.putString(Constants.EXTRA_RESULT, resultString);
				Message msg = Message.obtain(null,
						OperationActivity.MSG_DISPLAY_RESULT);
				msg.setData(resultData);
				mClients.get(OPERATION_ACTIVITY).send(msg);

			}
		} catch (RemoteException e) {
		} catch (AuthorizationException e) {
		} catch (InvocationException e) {
		} catch (CommunicationException e) {
		}
	}

	@Override
	public void finishedSearching(boolean entityFound, SearchParameter search) {
	}



	@Override
	public Set getDefaultOutgoingDiscoveryInfos(){
		DefaultClient defaultClient = new DefaultClient();

		return defaultClient.getDefaultOutgoingDiscoveryInfos();
	}







}
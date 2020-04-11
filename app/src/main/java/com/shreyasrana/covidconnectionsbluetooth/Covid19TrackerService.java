package com.shreyasrana.covidconnectionsbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.shreyasrana.covidconnectionsbluetooth.data.DataManager;
import com.shreyasrana.covidconnectionsbluetooth.data.InvalidContextException;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import java.util.ArrayList;
import java.util.UUID;

public class Covid19TrackerService extends JobIntentService {
    public static final String NEW_NEARBY_DEVICE = "new-nearby-device";
    /**
     * Unique job ID for this service.
     */
    private static final int JOB_ID = 1000;

    private static final long POLL_PERIOD = 10000; // milliseonds.
    private static final long DISCOVERABLE_DURATION = 300; // seconds; anything above 300 seconds is capped by the system.

    private String TAG="Covid19TrackerService";
    private boolean mDone;

    private  ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothConnectionManager mBTConnectionMgr;
    private UUID mDeviceUUID;
    private DataManager mDataManager;

    private static MainActivity parent;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work,  MainActivity mainActivity) {
        parent = mainActivity;
        Log.d("Covid19TrackerService", "enqueueWork");
        enqueueWork(context, Covid19TrackerService.class, JOB_ID, work);
    }

    private BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver3.onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                    mBTDevices.add(device);

                    Message msg = Message.obtain();
                    msg.obj = mBTDevices;
                    msg.setTarget(MainActivity.mDeviceListHandler);
                    msg.sendToTarget();

                    Log.d(TAG, "Discovered: " + device.getName() + " with address: " + device.getAddress() + " type: " + device.getType());

                    startSending(device);
                }
            }
        }
    };

    public Covid19TrackerService() {
        super();
    }

    @Override
    protected void onHandleWork(@NonNull Intent workIntent) {
        String dataString = workIntent.getDataString();

        Log.d(TAG, "onHandleWork: Initializing Service");
        init();

        Log.d(TAG, "onHandleWork: Starting discovery");
        startDiscovery();
    }

    private void init() {
//        parent = new MainActivity();

        mBTDevices = new ArrayList<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            mDataManager = DataManager.getInstance(getApplicationContext());
        } catch (InvalidContextException e) {
            Log.e(TAG, "init: Unable to initialize data manager", e);
            return;
        }

        mDeviceUUID = getDeviceUUIDLocal();

        // Enable bluetooth if not already enabled.
        enableBT();

        // Register receiver to handle BT discovery.
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

        // Create our BluetoothConnectionService and start accepting new Bluetooth connections.
        mBTConnectionMgr = new BluetoothConnectionManager(this);

        // Make my device discoverable over bluetooth.
        makeDiscoverable();
    }

    UUID getDeviceUUID() {
        return mDeviceUUID;
    }

    private UUID getDeviceUUIDLocal() {
        UUID deviceUUID = mDataManager.getOrInitUUID();
        Log.d(TAG, "getDeviceUUIDLocal: My uuid is: " + deviceUUID.toString());

        return deviceUUID;
    }

    // Going to make this device discoverable over BT by other devices.
    private void makeDiscoverable() {
        Log.d(TAG, "enableDiscoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2, intentFilter);
    }

    // Going to enable BT on this device.
    private void enableBT(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBTIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBTIntent);
        }
    }

    // Starts the discovery of bluetooth devices. As devices are discovered, the receiver will send
    // them messages.
    private void startDiscovery() {
        while (!mDone){
            try{
                parent.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(parent.getBaseContext(), "discovering devices", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (NullPointerException e){
                e.printStackTrace();
            }

//            Toast.makeText(getApplicationContext(), "discovering devices", Toast.LENGTH_LONG);
            discoverDevices();

            Log.d(TAG, "Going to wait for " + POLL_PERIOD + "ms.");
            try {
                synchronized (this) {
                    this.wait(POLL_PERIOD);
                }

                if (mDone) {
                    return;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Could not sleep: " + e);
            }
            Log.d(TAG, "Waited for " + POLL_PERIOD + "ms.");
        }
    }

    private void discoverDevices() {
        Log.d(TAG, "discoverDevices: Looking for nearby bluetooth devices.");

        mBTDevices.clear();

        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "discoverDevices: Canceling discovery. Will start new discovery.");
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothAdapter.startDiscovery();
    }

    /**
     * Start outbound connection to the specified bluetooth device.
     *
     * @param targetDevice The bluetooth device that the connection needs to be made to.
     */
    private void startSending(BluetoothDevice targetDevice) {
        Log.d(TAG, "Trying to connect to device: " + targetDevice.getName() + " with addresss: " +
                targetDevice.getAddress());
        mBTConnectionMgr.startClient(targetDevice);

        // Update UI if exists.
        Message msg = Message.obtain();
        msg.obj = targetDevice.getAddress();
        msg.setTarget(MainActivity.mSendingHandler);
        msg.sendToTarget();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying.");

        mBTConnectionMgr.stopServices();

        mDone = true;
        synchronized (this) {
            this.notifyAll();
        }
    }

    DataManager getDataManager() {
        return mDataManager;
    }
}

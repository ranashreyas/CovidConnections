package com.shreyasrana.covidconnectionsbluetooth.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.util.Log;

import com.shreyasrana.covidconnectionsbluetooth.ui.MainActivity;
import com.shreyasrana.covidconnectionsbluetooth.data.DataManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnMgr";

    private static final String appName = "Covid19Tracker";

    private Covid19TrackerService mTrackerService;
    private ConnectionAcceptor mConnectionAcceptor;
    private ClientConnector mClientConnector;
    private final BluetoothAdapter mBluetoothAdapter;
    private UUID mBTCommChannelUUID;
    private UUID mDeviceUUID;

    public BluetoothConnectionManager(Covid19TrackerService trackerService) {
        mTrackerService = trackerService;
        mDeviceUUID = mTrackerService.getDeviceUUID();

        mBTCommChannelUUID = getBTCommChannelUUID();

        // Initialize bluetooth adapter.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Create acceptor thread and start it.
        mConnectionAcceptor = new ConnectionAcceptor();
        mConnectionAcceptor.start();
    }

    void stopServices() {
        mConnectionAcceptor.cancel();
    }

    // TODO: Replace with a non static UUID.
    private UUID getBTCommChannelUUID() {
        return UUID.fromString("800001101-0000-1000-8000-00805F9B34FB");
    }

    // This thread is going to start a process to accept connections from other Bluetooth devices.
    private class ConnectionAcceptor extends Thread {
        private BluetoothServerSocket mmServerSocket;
        private boolean mDone = true;

        public ConnectionAcceptor() {
            try {
                mmServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, mBTCommChannelUUID);
                Log.d(TAG, "AcceptThread: Setting up server using: " + mBTCommChannelUUID);
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "AcceptThread: IOExeption | NullPointerException: ", e);
            }

            if (mmServerSocket != null) {
                mDone = false;
            }
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;
            if (mmServerSocket == null) {
                Log.e(TAG, "No ServerSocket to accept connections");
                return;
            }

            while (!mDone) {
                try {
                    //This is a blocking call and will only return on a successful connection or an exception.
                    Log.d(TAG, "run: RFCOM server socket start......");

//                    Message msg = Message.obtain();
//                    msg.obj = "Waiting for new connection...";
//                    msg.setTarget(MainActivity.mMessageHandler);
//                    msg.sendToTarget();

                    socket = mmServerSocket.accept();

                    Log.d(TAG, "run: RFCOM server socket accepted the connection");

                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: IOExeption: " + e.getMessage());
                }

                if (socket != null) {
                    processIncomingConnection(socket);
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public synchronized void cancel(){
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mDone = true;
                mmServerSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    public void startClient(BluetoothDevice targetDevice){
        Log.d(TAG, "startClient: Started.");

        mClientConnector = new ClientConnector(targetDevice);
        mClientConnector.start();
    }

    private class ClientConnector extends Thread {
        private BluetoothDevice mTargetDevice;
        private BluetoothSocket mSocket;
        public ClientConnector(BluetoothDevice targetDevice) {
            Log.d(TAG, "ClientConnector: started.");
            mTargetDevice = targetDevice;
        }

        public void run() {
            // See if we can find a record for this BluetoothDevice by address.
            DataManager dataManager = mTrackerService.getDataManager();
            if (dataManager.wasRecentlySeen(mTargetDevice.getAddress())) {
                Log.d(TAG, "startSending: Device: " + mTargetDevice.getName() + " with addresss: " +
                        mTargetDevice.getAddress() + " was recently seen. Ignoring.");
                return;
            }

            Log.i(TAG, "ClientConnector.run: mConnectThread ");
            if (mSocket != null) {
                // Close the old socket.
                try {
                    mSocket.close();
                } catch (IOException e) {
                    // Drop the exception.
                    Log.i(TAG, "ClientConnector.run: Error closing the old socket: ", e);
                }
                mSocket = null;
            }

            try {
                Log.d(TAG, "ClientConnector.run: Trying to create InsecureRcommSocket using UUID: " + mBTCommChannelUUID);
                mSocket = mTargetDevice.createInsecureRfcommSocketToServiceRecord(mBTCommChannelUUID);
            } catch (IOException e) {
                Log.e(TAG, "ClientConnector.run: Could not connect to device " + mTargetDevice.getAddress());
                Log.e(TAG, "ClientConnector.run: Could not connect to device.", e);
                return;
            }

            if (mSocket == null) {
                Log.e(TAG, "ClientConnector.run: Could not connect to device " + mTargetDevice.getAddress());
                return;
            }

            try {
                mSocket.connect();
                Log.d(TAG, "ClientConnector.run: ClientConnector connected.");
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1){
                    Log.e(TAG, "ClientConnector.run: Unable to close connection in socket ", e1);
                }
                Log.e(TAG, "ClientConnector.run: ClientConnector: Could not connect to Device " + mTargetDevice.getAddress());
                return;
            }

            // First write to the socket.
            try {
                writeToConnection(mSocket);
                readFromConnection((mTargetDevice == null ? null: mTargetDevice.getAddress()), mSocket);
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ClientConnector.run: Error writing, reading or closing the socket. ", e);
            }
        }
    }

    private void writeToConnection(BluetoothSocket targetSocket) throws IOException {
        // Create a JSON object.
        String date;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            date = dtf.format(now);
        } else {
            date = String.valueOf(System.currentTimeMillis());
        }

        JSONObject json = new JSONObject();
        String jsonString = date;
        try {
            json.put("date", date);
            json.put("device_id", mDeviceUUID.toString());
            jsonString = json.toString(4);
        } catch (JSONException e) {
            Log.e(TAG, "Exception creating JSON", e);
        }

        Log.d(TAG, "writeToConnection: Writing to outputstream: " + jsonString);
        byte[] bytes = jsonString.getBytes();

        OutputStream out = targetSocket.getOutputStream();
        out.write(bytes);
    }

    private void readFromConnection(String connectingDeviceAddress, BluetoothSocket socket)
            throws IOException {
        byte [] buffer = new byte[1024];

        InputStream in = socket.getInputStream();
        int bytes = in.read(buffer);

        String incomingMessage = new String(buffer, 0, bytes);

        Log.d(TAG, "Incoming message: " + incomingMessage);
//        Message msg = Message.obtain();
//        msg.obj = incomingMessage;
//        msg.setTarget(MainActivity.mMessageHandler);
//        msg.sendToTarget();

        // Write this device to the database.
        DataManager dataManager = mTrackerService.getDataManager();
        dataManager.searchAndAddNearbyDevice(connectingDeviceAddress, incomingMessage);
    }

    private void processIncomingConnection(BluetoothSocket socket) {
        Log.d(TAG, "connected: Starting.");

        // Pass this socket to a new thread to read data from this socket.
        ConnectionReader connectionReader = new ConnectionReader(socket);
        connectionReader.start();
    }

    private class ConnectionReader extends Thread {
        private final BluetoothSocket mSocket;

        private ConnectionReader(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread starting.");

            mSocket = socket;
        }

        public void run() {
            try {
                // Write first, read next.
                writeToConnection(mSocket);
                readFromConnection(null, mSocket);
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectionReader.run: Error writing, reading or closing the socket. ", e);
            }
        }
    }
}

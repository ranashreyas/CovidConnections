package com.shreyasrana.covidconnectionsbluetooth.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import com.shreyasrana.covidconnectionsbluetooth.services.Covid19TrackerService;
import com.shreyasrana.covidconnectionsbluetooth.R;
import com.shreyasrana.covidconnectionsbluetooth.data.DataManager;
import com.shreyasrana.covidconnectionsbluetooth.data.InvalidContextException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

//    public static Handler mMessageHandler;
//    public static Handler mSendingHandler;
//    public static Handler mDeviceListHandler;


//    private TextView rMessage;
//    private TextView rSend;
//    private Button rdiagnoseBtn;
//    private ListView lvNewDevices;
//
    private DeviceListAdapter mDeviceListAdapter;
    private DataManager mDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start background processes
        Log.d(TAG, "onCreate: called.");

        // FIRST: Initialize the data manager
        try {
            mDataManager = DataManager.getInstance(getApplicationContext());
        } catch (InvalidContextException e) {
            Log.e(TAG, "onCreate: Unable to initialize data manager, HALTING!!!", e);
            return;
        }

        // SECOND: Check BT permissions in manifest
        checkBTPermissions();

        // THIRD: Draw screen.
        setContentView(R.layout.main_page);
//        setContentView(R.layout.activity_main);

//        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
//        rMessage = findViewById(R.id.rMessage);
//        rSend = findViewById(R.id.rSend);
//        rdiagnoseBtn = findViewById(R.id.diagnoseBtn);

//        mMessageHandler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//                String message = (String) msg.obj;
//                rMessage.setText("Received Message: " + message);
//            }
//        };

//        mSendingHandler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//                String message = (String) msg.obj;
//                rSend.setText("Sending To: " + message);
//            }
//        };
//        mDeviceListHandler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(Message msg) {
//                ArrayList<BluetoothDevice> BTDevices = (ArrayList<BluetoothDevice>) msg.obj;
//                mDeviceListAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.device_adapter_view, BTDevices);
//                lvNewDevices.setAdapter(mDeviceListAdapter);
//            }
//        };
    }

    private void initServices() {
        Intent serviceIntent = new Intent();
        Covid19TrackerService.enqueueWork(getApplicationContext(), serviceIntent, this);

        // Enable notifications.
        enableIncomingNotifications();
    }

    private void enableIncomingNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String topicName = getString(R.string.default_notification_topic_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    topicName, NotificationManager.IMPORTANCE_LOW));
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

        // Get token
        // [START retrieve_current_token]
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Set this token in the remote instance.
                        mDataManager.setDeviceToken(token);

                        // Log and toast
                        Log.d(TAG, "Token: " + token);
//                        Toast.makeText(MainActivity.this, "Token: " + token, Toast.LENGTH_SHORT).show();
                    }
                });
        // [END retrieve_current_token]

        Log.d(TAG, "Subscribing to Covid19 topic");
        FirebaseMessaging.getInstance().subscribeToTopic("Covid19");
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Context appContext = getApplicationContext();

            int permissionCheck = appContext.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += appContext.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: User granted the permission to use Bluetooth.");
                // THIRD: Initialize DeviceID and other services.
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        mDataManager.getOrInitUUID();
                        initServices();
                        return null;
                    }
                }.execute();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: User didn't grant permission to use Bluetooth. App cannot be used.");
            }
        }
    }

    public void updateFirebase(View v) {
        startActivity(new Intent(MainActivity.this, Pop.class));
        notifyNearbyDevices();
    }

    private void notifyNearbyDevices() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mDataManager.notifyNearbyDevices();
                return null;
            }
        }.execute();
    }
}
package com.shreyasrana.covidconnectionsbluetooth.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.JsonReader;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.room.Room;

/**
 * Singleton class used to manage data in the DB
 */
public class DataManager {
    private static String TAG = "DataManager";

    private static final  String KEY = "UUID_PREFERENCE_KEY";
    private static final  String UUID_KEY = "UUID_KEY";

    private static final String APP_DATA = "app_data";
    private static final String DEVICES = "devices";
    private static final String INITIALIZED = "initialized";
    private static final String COVID_DIAGNOSED = "covid_diagnosed";
    private static final String DEVICE_TOKEN = "device_token";
    private static final String NEARBY_DEVICES = "nearby_devices";
    private static final String LAST_SEEN = "last_seen";

    //    private static long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;
    private static long MILLIS_PER_DAY = 60 * 1000L;
    private static long MILLIS_21_DAYS = 21 * 24 * 60 * 60 * 1000L;

    private static DataManager sDataManager;

    private NearbyDeviceDAO mNearbyDeviceDOA;
    private SharedPreferences mSharedPref;
    private DatabaseReference mFirebaseDB;

    private DataManager(Context context) {
        Log.i(TAG, "Creating/looking up the nearby-device-db.");
        NearbyDeviceDB mNearbyDeviceDB = Room.databaseBuilder(context,
                NearbyDeviceDB.class, "nearby-device-db").build();

        mNearbyDeviceDOA = mNearbyDeviceDB.nearbyDeviceDAO();
        mSharedPref = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);

        FirebaseDatabase firebaseDB = FirebaseDatabase.getInstance();
        mFirebaseDB = firebaseDB.getReference(APP_DATA);

        Log.i(TAG, "Created the DB.");
    }

    /**
     * Gets the instance of the DataManager.
     *
     * @param context
     * @return
     * @throws InvalidContextException
     */
    public static DataManager getInstance(Context context)
            throws InvalidContextException {
        if (context == null) {
            throw new InvalidContextException("missing context");
        }

        synchronized (DataManager.class) {
            if (sDataManager == null) {
                sDataManager = new DataManager(context);
            }
            return sDataManager;
        }
    }

    public void searchAndAddNearbyDevice(String targetMacAddress, String incomingMessage) throws IOException {
        Log.d(TAG, "searchAndAddNearbyDevice: Checking and adding device with address " + targetMacAddress +
                " data " + incomingMessage);
        JsonReader reader = new JsonReader(new StringReader(incomingMessage));
        String deviceId = "";
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("date")) {
                // read and drop it.
                reader.nextString();
            } else if (name.equals("device_id")) {
                deviceId = reader.nextString();
            }
        }

        if (deviceId.length() == 0) {
            // Do nothing.
            Log.e(TAG, "searchAndAddNearbyDevice: No device id read.");
        }

        NearbyDevice nearbyDevice = mNearbyDeviceDOA.findByDeviceId(deviceId);

        Date now = new Date();
        if (nearbyDevice == null) {
            // We didn't find this device, so write it along with today's date.
            Log.d(TAG, "searchAndAddNearbyDevice: Device with id " + deviceId + " not found. Adding.");
            nearbyDevice = new NearbyDevice();
            nearbyDevice.macAddress = targetMacAddress;
            nearbyDevice.deviceId = deviceId;
            nearbyDevice.lastSeen = now;

            mNearbyDeviceDOA.insert(nearbyDevice);
        } else {
            Log.d(TAG, "searchAndAddNearbyDevice: Found the Device with id " + deviceId +
                    " with mac: " + nearbyDevice.macAddress + " was last seen: " + nearbyDevice.lastSeen);
            Log.d(TAG, "searchAndAddNearbyDevice: Updating last seen");
            nearbyDevice.lastSeen = now;
            mNearbyDeviceDOA.update(nearbyDevice);
//            // Check when was the device discovered.
//            if (now.getTime() - nearbyDevice.lastSeen.getTime() >= MILLIS_PER_DAY) {
//                // The device was discovered more than 24 hours back, refresh it.
//            } else {
//                // Don't do anything, leave this device unchanged.
//            }
        }
    }

    public boolean wasRecentlySeen(String address) {
        NearbyDevice nearbyDevice = mNearbyDeviceDOA.findByMacAddress(address);
        if (nearbyDevice == null) {
            // We didn't find this device.
            Log.d(TAG, "wasRecentlySeen: No device with address " + address + " found.");
            return false;
        }

        // Check when we last communicated with this device.
        Date now = new Date();
        if (now.getTime() - nearbyDevice.lastSeen.getTime() >= MILLIS_PER_DAY) {
            // The device was discovered more than 24 hours back, return false.
            Log.d(TAG, "wasRecentlySeen: Device with address " + address +
                    " was last seen more than a day back at " + nearbyDevice.lastSeen);
            return false;
        }

        Log.d(TAG, "wasRecentlySeen: Device with address " + address +
                " was last seen less than a day back at " + nearbyDevice.lastSeen);
        return true;
    }

    public List<NearbyDeviceFB> getDevicesNearbyIn21Days() {
        Date now = new Date();

        List<NearbyDevice> allDevices = mNearbyDeviceDOA.getAll();
        List<NearbyDeviceFB> devicesNearbyIn21Days = new ArrayList<>();

        for (NearbyDevice device: allDevices) {
            if (now.getTime() - device.lastSeen.getTime() <= MILLIS_21_DAYS) {
                devicesNearbyIn21Days.add(new NearbyDeviceFB(device.deviceId, Converters.dateToTimestamp(device.lastSeen)));
            }
        }

        return devicesNearbyIn21Days;
    }

    public UUID getOrInitUUID() {
        String uuidStr = mSharedPref.getString(UUID_KEY, "null");
        if(uuidStr.equals("null")) {
             return createUUID();
        } else {
            return UUID.fromString(uuidStr);
        }
    }

    private UUID createUUID() {
        Log.d(TAG, "createUUID: Writing to SharedPrefs");
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();

        UUID uuid = UUID.randomUUID();
        String uuidStr = uuid.toString();
        preferencesEditor.putString(UUID_KEY, uuidStr);
        preferencesEditor.apply();

        Log.d(TAG, "createUUID: Initializing the device in FB");
        mFirebaseDB.child(DEVICES).child(uuidStr).child(INITIALIZED).setValue(Converters.dateToTimestamp(new Date()));

        return uuid;
    }

    public void setDeviceToken(String deviceToken) {
        Log.d(TAG, "setDeviceToken: Writing device token " + deviceToken + " to FB");
        String uuidStr = getOrInitUUID().toString();
        mFirebaseDB.child(DEVICES).child(uuidStr).child(DEVICE_TOKEN).setValue(deviceToken);
    }

    public void notifyNearbyDevices() {
        Log.d(TAG, "notifyNearbyDevices: Notifying nearby devices.");

        String uuidStr = getOrInitUUID().toString();

        List<NearbyDeviceFB> nearbyDevices = getDevicesNearbyIn21Days();
        mFirebaseDB.child(DEVICES).child(uuidStr).child(COVID_DIAGNOSED).setValue(Converters.dateToTimestamp(new Date()));
        mFirebaseDB.child(DEVICES).child(uuidStr).child(NEARBY_DEVICES).setValue(nearbyDevices);
    }

    @IgnoreExtraProperties
    public class NearbyDeviceFB {
        public String deviceId;
        public String lastSeen;

        public NearbyDeviceFB() {
            // Default constructor required for calls to DataSnapshot.getValue(NearbyDeviceFB.class)
        }

        public NearbyDeviceFB(String deviceId, String lastSeen) {
            this.deviceId = deviceId;
            this.lastSeen = lastSeen;
        }
    }
}

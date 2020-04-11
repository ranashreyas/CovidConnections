package com.shreyasrana.covidconnectionsbluetooth.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {NearbyDevice.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class NearbyDeviceDB extends RoomDatabase {
    public abstract NearbyDeviceDAO nearbyDeviceDAO();
}

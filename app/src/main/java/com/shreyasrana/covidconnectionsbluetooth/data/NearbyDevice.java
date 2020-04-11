package com.shreyasrana.covidconnectionsbluetooth.data;

import java.util.Date;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class NearbyDevice {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "mac_address", index = true)
    public String macAddress;

    @ColumnInfo(name = "device_id", index = true)
    public String deviceId;

    @ColumnInfo(name = "last_seen")
    public Date lastSeen;
}

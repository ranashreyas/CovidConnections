package com.shreyasrana.covidconnectionsbluetooth.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface NearbyDeviceDAO {
    @Query("SELECT * FROM nearbydevice")
    List<NearbyDevice> getAll();

    @Query("SELECT * FROM nearbydevice WHERE id IN (:ids)")
    List<NearbyDevice> loadAllByIds(int[] ids);

    @Query("SELECT * FROM nearbydevice WHERE mac_address LIKE :macAddress LIMIT 1")
    NearbyDevice findByMacAddress(String macAddress);

    @Query("SELECT * FROM nearbydevice WHERE device_id LIKE :deviceId LIMIT 1")
    NearbyDevice findByDeviceId(String deviceId);

    @Insert
    void insertAll(NearbyDevice... nearbyDevices);

    @Insert
    void insert(NearbyDevice nearbyDevice);

    @Update
    void update(NearbyDevice nearbyDevice);

    @Delete
    void delete(NearbyDevice nearbyDevice);
}
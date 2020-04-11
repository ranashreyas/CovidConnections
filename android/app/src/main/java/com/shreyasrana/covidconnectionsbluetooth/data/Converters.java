package com.shreyasrana.covidconnectionsbluetooth.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.room.TypeConverter;

public class Converters {
    private static String PATTERN = "yyyy-MM-dd HH:mm:ss.SSSZ";
    @TypeConverter
    public static Date fromTimestamp(String value) {
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN);
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    @TypeConverter
    public static String dateToTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(PATTERN);
        return date == null ? null : sdf.format(date);
    }
}
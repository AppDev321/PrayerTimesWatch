package com.sommayah.myprayertimes.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sommayahsoliman on 3/11/16.
 */
public class PrayerDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "prayer.db";
    private static final int DATABASE_VERSION = 2;

    public PrayerDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
// Create a table to hold locations.  A location consists of the string supplied in the
        // location setting, the city name, and the latitude and longitude

        final String SQL_CREATE_PRAYER_TABLE = "CREATE TABLE " + PrayerContract.PrayerEntry.TABLE_NAME + " (" +
                PrayerContract.PrayerEntry._ID + " INTEGER PRIMARY KEY," +
                PrayerContract.PrayerEntry.COLUMN_DATE + " TEXT UNIQUE NOT NULL, " +
                PrayerContract.PrayerEntry.COLUMN_FAJR + " TEXT NOT NULL, " +
                PrayerContract.PrayerEntry.COLUMN_SUNRISE + " TEXT NOT NULL, "+
                PrayerContract.PrayerEntry.COLUMN_DHUHR + " TEXT NOT NULL, " +
                PrayerContract.PrayerEntry.COLUMN_ASR + " TEXT NOT NULL, " +
                PrayerContract.PrayerEntry.COLUMN_MAGHRIB + " TEXT NOT NULL, " +
                PrayerContract.PrayerEntry.COLUMN_ISHA + " TEXT NOT NULL " +
                " );";

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

package com.sommayah.myprayertimes.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.sommayah.myprayertimes.LoadPrayersAsyncTask;
import com.sommayah.myprayertimes.R;

import java.util.Calendar;
import java.util.Date;

public class PassiveLocationChangeReceiver extends BroadcastReceiver {
    public PassiveLocationChangeReceiver() {
    }

    PrayerAlarmReceiver alarm = new PrayerAlarmReceiver();

    /**
     * When a new location is received, extract it from the Intent and use
     * it to start the Service used to update the list of nearby places.
     *
     * This is the Passive receiver, used to receive Location updates from
     * third party apps when the Activity is not visible.
     */
    @Override
    public void onReceive (Context context, Intent intent){
        String key = LocationManager.KEY_LOCATION_CHANGED;
        Location location = null;

        if (intent.hasExtra(key)) {
            // This update came from Passive provider, so we can extract the location
            // directly.
            location = (Location) intent.getExtras().get(key);
            if (location != null) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(context.getResources().getString(R.string.pref_location_key), "");

                // Also store the latitude and longitude so that we can use these to get a precise
                // result from our weather service. We cannot expect the weather service to
                // understand addresses that Google formats.
                editor.putFloat(context.getResources().getString(R.string.pref_location_latitude),
                        (float) location.getLatitude());
                editor.putFloat(context.getResources().getString(R.string.pref_location_longitude),
                        (float) location.getLongitude());
                editor.commit();
                //get new prayer data
                Date now = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);
                new LoadPrayersAsyncTask(context,cal).execute();
                alarm.cancelAlarm(context);
                alarm.addPrayerAlarm(context);
            }
        }
    }
}

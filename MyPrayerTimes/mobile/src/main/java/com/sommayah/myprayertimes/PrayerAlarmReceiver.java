package com.sommayah.myprayertimes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.content.WakefulBroadcastReceiver;

public class PrayerAlarmReceiver extends WakefulBroadcastReceiver {
    public static final String ACTION_PRAYER_TIME_ALARM = "com.sommayah.myprayertimes.ACTION_PRAYER_TIME_ALARM";
    public static final String EXTRA_PRAYER_NAME = "prayer_name";
    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmManager;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;
    public PrayerAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving



        // Start the service, keeping the device awake while it is launching.
        String prayerName =  intent.getStringExtra(EXTRA_PRAYER_NAME);
        if(prayerName == null)
            prayerName = ""; // in case error in prayer name

        if(Utility.isAlarmEnabled(context)) {
            Intent sendNotificationIntent = new Intent(context, PrayerNotificationService.class);
            sendNotificationIntent.putExtra(EXTRA_PRAYER_NAME, prayerName);
            startWakefulService(context, sendNotificationIntent);
        }
        //set the next prayer alarm
        addPrayerAlarm(context);
    }

    public void addPrayerAlarm(Context context){
        // Enable {@code SampleBootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, PrayerOnBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

    }

    public void cancelAlarm(Context context) {

    }
}

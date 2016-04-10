package com.sommayah.myprayertimes.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sommayah.myprayertimes.Utility;

public class PrayerOnBootReceiver extends BroadcastReceiver {
    PrayerAlarmReceiver prayerAlarm = new PrayerAlarmReceiver();
    public PrayerOnBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            if (Utility.isAlarmEnabled(context)) {
                prayerAlarm.addPrayerAlarm(context);
            }
        } else if (action.equals("android.intent.action.TIMEZONE_CHANGED") ||
                action.equals("android.intent.action.TIME_SET") ||
                action.equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            // Our location could have changed, which means time calculations may be different
            // now so cancel the alarm and set it again.
            prayerAlarm.cancelAlarm(context);
            prayerAlarm.addPrayerAlarm(context);
        }
    }

}

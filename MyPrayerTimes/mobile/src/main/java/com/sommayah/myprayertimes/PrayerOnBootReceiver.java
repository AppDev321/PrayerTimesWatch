package com.sommayah.myprayertimes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
            if (Utility.isAlarmEnabled(context)) {
                prayerAlarm.cancelAlarm(context);
                prayerAlarm.addPrayerAlarm(context);
            }

        }
    }

}

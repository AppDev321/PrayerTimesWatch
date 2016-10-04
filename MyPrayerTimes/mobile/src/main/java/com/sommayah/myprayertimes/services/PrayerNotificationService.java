package com.sommayah.myprayertimes.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.sommayah.myprayertimes.MainActivity;
import com.sommayah.myprayertimes.R;
import com.sommayah.myprayertimes.Utility;
import com.sommayah.myprayertimes.broadcastReceivers.PrayerAlarmReceiver;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PrayerNotificationService extends IntentService {
    private final String TAG = PrayerNotificationService.class.getSimpleName();
    private Notification.Builder prayerNotificationBuilder;
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_NEXT_PRAYER_UPDATED = "com.sommayah.myprayertimes.ACTION_NEXT_PRAYER_UPDATED";
    private NotificationManager mNotificationManager;
    private PendingIntent mAlarmIntent;

    public PrayerNotificationService() {
        super("PrayerNotificationService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String name= intent.getStringExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME);
            long time = intent.getLongExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, -1);
            broadcastNotification(getApplicationContext(),name, time);
            // Release the wake lock provided by the BroadcastReceiver.
            PrayerAlarmReceiver.completeWakefulIntent(intent);
            // END_INCLUDE(service_onhandle)

        }
    }



    private void broadcastNotification(Context context, String prayer, long time) {
        //ss:incorporate the preferences later
        if(!prayer.equals(getString(R.string.sunrise)) && Utility.isAlarmEnabled(context)) {

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setColor(context.getResources().getColor(R.color.colorPrimary))
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("Time for prayer")
                            .setContentText("Time for " + prayer + " prayer")
                            .setTicker("Time to Pray")
                            .setAutoCancel(true);

            if (time != -1)
                mBuilder.setWhen(time);

            Intent startActivityIntent = new Intent(this, MainActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntent(startActivityIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            Uri defaultRingURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            //check which prayer and check kind of notification sound accordingly
            int notification = getPrayerNotificationSound(prayer);
            String uriString = preferences.getString(getString(R.string.pref_notification_ringtone_key), defaultRingURI.toString());
            if(notification == 1){
                uriString = "android.resource://"
                        + context.getPackageName() + "/" + R.raw.allahakbar;
            }else if(notification == 2){
                uriString = "android.resource://"
                        + context.getPackageName() + "/" + R.raw.azan20;
            }else if(notification == 3){
                uriString = "android.resource://"
                        + context.getPackageName() + "/" + R.raw.azan20;
            }
            if(uriString != null){
                mBuilder.setSound(Uri.parse(uriString));
            }
//            String ringURIString = preferences.getString(getString(R.string.pref_notification_ringtone_key), defaultRingURI.toString());
//            String adhanURIString = "android.resource://"
//                    + context.getPackageName() + "/" + R.raw.allahakbar;
////            if (ringURIString != null) {
////                mBuilder.setSound(Uri.parse(ringURIString));
////            }
//            if(adhanURIString != null){
//                mBuilder.setSound(Uri.parse(adhanURIString));
//            }
            long[] vibrate = new long[]{100, 100, 100};
            //check if user has vibration enabled.
            if (Utility.isVibrateEnabled(context)) {
                mBuilder.setVibrate(vibrate);
            }
            final NotificationManager notificationManager
                    = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID,
                    mBuilder.build());

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, RemoveNotificationService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 1, alarmIntent, 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 15000 * 60, pendingIntent); //remove notifications after 15 minutes
        }
        updateWidgets();
    }

    public void updateWidgets(){
        Context context = getApplicationContext();
        Intent nextPrayerUpdatedIntent = new Intent(ACTION_NEXT_PRAYER_UPDATED).setPackage(context.getPackageName());
        context.sendBroadcast(nextPrayerUpdatedIntent);

    }

    private int getPrayerNotificationSound(String prayer){
        int notification = 0;
        SharedPreferences prefs
                = PreferenceManager.getDefaultSharedPreferences(this);
        if(prayer.equals(getString(R.string.fajr))){
            notification = Integer.parseInt(prefs.getString(getString(R.string.pref_fajr_notification_methods_key),"0"));
        }else if(prayer.equals(getString(R.string.dhuhr))){
            notification = Integer.parseInt(prefs.getString(getString(R.string.pref_dhuhr_notification_methods_key),"0"));
        }else if(prayer.equals(getString(R.string.asr))){
            notification = Integer.parseInt(prefs.getString(getString(R.string.pref_asr_notification_methods_key),"0"));
        }else if(prayer.equals(getString(R.string.maghrib))){
            notification = Integer.parseInt(prefs.getString(getString(R.string.pref_maghrib_notification_methods_key),"0"));
        }else if(prayer.equals(getString(R.string.isha))){
            notification = Integer.parseInt(prefs.getString(getString(R.string.pref_isha_notification_methods_key),"0"));
        }
        return notification;
    }

}



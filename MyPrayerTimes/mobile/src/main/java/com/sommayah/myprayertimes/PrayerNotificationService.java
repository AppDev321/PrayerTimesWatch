package com.sommayah.myprayertimes;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

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
    private NotificationManager mNotificationManager;

    private PendingIntent mAlarmIntent;


    public PrayerNotificationService() {
        super("PrayerNotificationService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String name= intent.getStringExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME);
            broadcastNotification(getApplicationContext(),name);
            // Release the wake lock provided by the BroadcastReceiver.
            PrayerAlarmReceiver.completeWakefulIntent(intent);
            // END_INCLUDE(service_onhandle)

        }
    }



    private void broadcastNotification(Context context, String prayer) {
        //ss:incorporate the preferences later

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setColor(context.getResources().getColor(R.color.primary_light))
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Time for prayer")
                        .setContentText("Time for " + prayer + " prayer.")
                        .setTicker("Time to Pray:ticker")
                        .setAutoCancel(true);

        Intent startActivityIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(startActivityIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        Uri ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(ringURI);
        long[] vibrate = new long[] {100, 100, 100 };
        mBuilder.setVibrate(vibrate);
        final NotificationManager notificationManager
                = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID,
                mBuilder.build());

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                notificationManager.cancel(NOTIFICATION_ID);
            }

        }, 15000*60); //remove notification after 15 minutes
    }
}



package com.sommayah.myprayertimes.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.sommayah.myprayertimes.services.PrayerNotificationService;

public class RemoveNotificationService extends Service {
    public RemoveNotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(PrayerNotificationService.NOTIFICATION_ID);
        return super.onStartCommand(intent, flags, startId);
    }
}
